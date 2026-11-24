package com.nithishramurthy.farm.fresh.service;

import com.nithishramurthy.farm.fresh.dto.CatalogProduct;
import com.nithishramurthy.farm.fresh.dto.ProductSearchResult;
import com.nithishramurthy.farm.fresh.repository.OrderItemRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Search, filters and sorting for the shop.
 *
 * It works on the visible catalogue in memory instead of in SQL, on purpose:
 * the price a customer sees depends on today's discount rules, which are worked
 * out in Java (ProductService), so "under ₹100" and "price: low to high" have
 * to use that price, not the stored one. A farm shop has a few hundred
 * products, so this takes well under a millisecond. With tens of thousands, the
 * next step is storing today's price in a column, or a search engine. See
 * design decision 13.
 */
@Service
public class ProductSearchService {

    public static final int DEFAULT_PAGE_SIZE = 24;
    public static final int MAX_PAGE_SIZE = 60;

    private final ProductService productService;
    private final OrderItemRepository orderItemRepository;

    public ProductSearchService(ProductService productService, OrderItemRepository orderItemRepository) {
        this.productService = productService;
        this.orderItemRepository = orderItemRepository;
    }

    /** The six ways the shop can sort results. */
    public enum Sort {
        RELEVANCE, POPULAR, PRICE_ASC, PRICE_DESC, DISCOUNT, NAME;

        /** "price-asc" -> PRICE_ASC. Unknown values fall back to relevance. */
        public static Sort from(String value) {
            if (value == null || value.isBlank()) {
                return RELEVANCE;
            }
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
            } catch (IllegalArgumentException e) {
                return RELEVANCE;
            }
        }
    }

    /** Everything the search box and filters can ask for. Null or blank means "no filter". */
    public record Query(String text, String category, BigDecimal minPrice, BigDecimal maxPrice,
                        boolean inStockOnly, Sort sort, int page, int size) {
    }

    /** Runs one search: match the words, filter, count per aisle, sort, then cut out one page. */
    @Transactional(readOnly = true)
    public ProductSearchResult search(Query query) {
        List<String> words = words(query.text());
        List<CatalogProduct> all = productService.listCatalog();

        // 1. Text match, with a score for "best match" ordering
        Map<Long, Integer> scores = new HashMap<>();
        List<CatalogProduct> matched = new ArrayList<>();
        for (CatalogProduct product : all) {
            int score = score(product, words);
            if (score > 0) {
                scores.put(product.id(), score);
                matched.add(product);
            }
        }

        // 2. Price and stock filters. Aisle counts are taken here, before the aisle
        //    filter, so the chips say how many you'd get by switching aisle.
        List<CatalogProduct> filtered = matched.stream()
                .filter(p -> query.minPrice() == null || p.price().compareTo(query.minPrice()) >= 0)
                .filter(p -> query.maxPrice() == null || p.price().compareTo(query.maxPrice()) <= 0)
                .filter(p -> !query.inStockOnly() || p.inStock())
                .toList();
        Map<String, Long> categoryCounts = filtered.stream()
                .filter(p -> p.category() != null)
                .collect(Collectors.groupingBy(CatalogProduct::category, TreeMap::new, Collectors.counting()));

        // 3. Aisle
        List<CatalogProduct> results = filtered.stream()
                .filter(p -> query.category() == null || query.category().isBlank() || query.category().equals(p.category()))
                .sorted(comparator(query.sort(), words.isEmpty(), scores))
                .toList();

        // 4. One page
        int size = Math.max(1, Math.min(query.size(), MAX_PAGE_SIZE));
        int totalPages = (int) Math.ceil(results.size() / (double) size);
        int page = Math.max(0, query.page());
        List<CatalogProduct> items = results.stream().skip((long) page * size).limit(size).toList();

        BigDecimal lowest = matched.stream().map(CatalogProduct::price).min(Comparator.naturalOrder()).orElse(null);
        BigDecimal highest = matched.stream().map(CatalogProduct::price).max(Comparator.naturalOrder()).orElse(null);
        String didYouMean = results.isEmpty() && !words.isEmpty() ? closestName(query.text(), all) : null;

        return new ProductSearchResult(items, page, size, results.size(), totalPages, categoryCounts,
                lowest, highest, didYouMean);
    }

    /**
     * How well a product matches. Every word must match somewhere, or the score is 0.
     * A match in the name counts most, then the aisle, then the description.
     * With no words, everything matches with score 1.
     */
    static int score(CatalogProduct product, List<String> words) {
        if (words.isEmpty()) {
            return 1;
        }
        List<String> nameWords = words(product.name());
        String name = String.join(" ", nameWords);
        List<String> categoryWords = words(product.category() == null ? "" : product.category().replace('-', ' '));
        List<String> descriptionWords = words(product.description());

        int total = 0;
        for (String word : words) {
            int best = 0;
            if (nameWords.contains(word)) best = 30;
            else if (nameWords.stream().anyMatch(w -> w.startsWith(word))) best = 20;
            else if (name.contains(word)) best = 12;
            else if (categoryWords.stream().anyMatch(w -> w.startsWith(word))) best = 8;
            else if (descriptionWords.stream().anyMatch(w -> w.startsWith(word))) best = 3;
            if (best == 0) {
                return 0;
            }
            total += best;
        }
        if (name.equals(String.join(" ", words))) {
            total += 50; // typed the exact name
        }
        return total;
    }

    private Comparator<CatalogProduct> comparator(Sort sort, boolean noText, Map<Long, Integer> scores) {
        Comparator<CatalogProduct> soldOutLast = Comparator.comparing(p -> !p.inStock());
        Comparator<CatalogProduct> byName = Comparator.comparing(p -> p.name().toLowerCase(Locale.ROOT));

        Comparator<CatalogProduct> chosen = switch (noText && sort == Sort.RELEVANCE ? Sort.POPULAR : sort) {
            case RELEVANCE -> Comparator.comparing((CatalogProduct p) -> scores.getOrDefault(p.id(), 0)).reversed();
            case POPULAR -> {
                Map<Long, Long> sold = unitsSoldLast30Days();
                yield Comparator.comparing((CatalogProduct p) -> sold.getOrDefault(p.id(), 0L)).reversed();
            }
            case PRICE_ASC -> Comparator.comparing(CatalogProduct::price);
            case PRICE_DESC -> Comparator.comparing(CatalogProduct::price).reversed();
            case DISCOUNT -> Comparator.comparing((CatalogProduct p) ->
                    p.offPercent() == null ? BigDecimal.ZERO : p.offPercent()).reversed();
            case NAME -> byName;
        };
        return soldOutLast.thenComparing(chosen).thenComparing(byName);
    }

    private Map<Long, Long> unitsSoldLast30Days() {
        Map<Long, Long> sold = new HashMap<>();
        for (Object[] row : orderItemRepository.unitsSoldSince(LocalDateTime.now().minusDays(30))) {
            sold.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return sold;
    }

    /** The product name closest to what was typed, if it's only a typo or two away. */
    static String closestName(String typed, List<CatalogProduct> products) {
        String target = String.join(" ", words(typed));
        String best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (CatalogProduct product : products) {
            for (String candidate : candidates(product.name())) {
                int distance = editDistance(target, candidate);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = candidate;
                }
            }
        }
        int allowed = target.length() <= 4 ? 1 : 2;
        return best != null && bestDistance <= allowed ? best : null;
    }

    /** The whole name and each word in it, e.g. "Baby Carrot" -> ["baby carrot", "baby", "carrot"]. */
    private static List<String> candidates(String name) {
        List<String> parts = words(name);
        List<String> out = new ArrayList<>();
        out.add(String.join(" ", parts));
        out.addAll(parts);
        return out;
    }

    /**
     * Lower case, accents removed, split on anything that isn't a letter or digit,
     * and simple plurals made singular, so "Carrots", "carrot" and "CARROT" all match.
     */
    static List<String> words(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String plain = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return Arrays.stream(plain.split("[^\\p{L}\\p{N}]+"))
                .filter(w -> !w.isEmpty())
                .map(ProductSearchService::singular)
                .toList();
    }

    private static String singular(String word) {
        if (word.length() > 4 && word.endsWith("ies")) return word.substring(0, word.length() - 3) + "y"; // berries
        if (word.length() > 4 && word.endsWith("oes")) return word.substring(0, word.length() - 2);       // tomatoes
        if (word.length() > 3 && word.endsWith("s") && !word.endsWith("ss")) return word.substring(0, word.length() - 1);
        return word;
    }

    /** Levenshtein distance: the fewest single-letter edits that turn one word into the other. */
    static int editDistance(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) previous[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }
}
