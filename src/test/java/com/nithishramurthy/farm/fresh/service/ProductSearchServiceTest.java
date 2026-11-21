package com.nithishramurthy.farm.fresh.service;

import com.nithishramurthy.farm.fresh.dto.CatalogProduct;
import com.nithishramurthy.farm.fresh.dto.ProductSearchResult;
import com.nithishramurthy.farm.fresh.repository.OrderItemRepository;
import com.nithishramurthy.farm.fresh.service.ProductSearchService.Query;
import com.nithishramurthy.farm.fresh.service.ProductSearchService.Sort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductSearchServiceTest {

    private final ProductService productService = mock(ProductService.class);
    private final OrderItemRepository orderItems = mock(OrderItemRepository.class);
    private final ProductSearchService search = new ProductSearchService(productService, orderItems);

    @BeforeEach
    void catalogue() {
        when(productService.listCatalog()).thenReturn(List.of(
                product(1, "Carrot", "vegetables", "Sweet Ooty carrots", "60", null, 10),
                product(2, "Baby Carrot", "vegetables", "Small and crunchy", "90", null, 0),
                product(3, "Strawberry", "fruits", "Picked this morning", "180", "20", 5),
                product(4, "Carrot Cake Mix", "bakery", "Just add eggs", "250", null, 3),
                product(5, "Tomato", "vegetables", "Country tomatoes", "40", "10", 8)));
        when(orderItems.unitsSoldSince(any())).thenReturn(List.of(
                new Object[]{5L, 40L}, new Object[]{3L, 12L}, new Object[]{1L, 3L}));
    }

    @Test
    void findsPluralsAndIgnoresCase() {
        assertThat(names(run("CARROTS", null, Sort.RELEVANCE))).contains("Carrot", "Baby Carrot", "Carrot Cake Mix");
        assertThat(names(run("strawberries", null, Sort.RELEVANCE))).containsExactly("Strawberry");
        assertThat(names(run("tomatoes", null, Sort.RELEVANCE))).containsExactly("Tomato");
    }

    @Test
    void bestMatchFirstAndSoldOutLast() {
        // the exact name wins; Baby Carrot is sold out, so it goes to the end
        assertThat(names(run("carrot", null, Sort.RELEVANCE))).containsExactly("Carrot", "Carrot Cake Mix", "Baby Carrot");
    }

    @Test
    void everyWordHasToMatch() {
        assertThat(names(run("carrot cake", null, Sort.RELEVANCE))).containsExactly("Carrot Cake Mix");
        assertThat(names(run("carrot banana", null, Sort.RELEVANCE))).isEmpty();
    }

    @Test
    void matchesTheAisleAndTheDescription() {
        assertThat(names(run("fruit", null, Sort.RELEVANCE))).containsExactly("Strawberry");
        assertThat(names(run("crunchy", null, Sort.RELEVANCE))).containsExactly("Baby Carrot");
    }

    @Test
    void filtersByAislePriceAndStock() {
        ProductSearchResult result = search.search(new Query(null, "vegetables", new BigDecimal("50"),
                new BigDecimal("100"), true, Sort.PRICE_ASC, 0, 24));
        assertThat(names(result)).containsExactly("Carrot");
    }

    @Test
    void aisleCountsIgnoreTheChosenAisle() {
        ProductSearchResult result = run("carrot", "bakery", Sort.RELEVANCE);
        assertThat(names(result)).containsExactly("Carrot Cake Mix");
        assertThat(result.categoryCounts()).containsEntry("vegetables", 2L).containsEntry("bakery", 1L);
    }

    @Test
    void sortsByPriceDiscountAndPopularity() {
        assertThat(names(run(null, "vegetables", Sort.PRICE_DESC))).containsExactly("Carrot", "Tomato", "Baby Carrot");
        assertThat(names(run(null, null, Sort.DISCOUNT)).subList(0, 2)).containsExactly("Strawberry", "Tomato");
        // no search words: most sold in the last 30 days first
        assertThat(names(run(null, null, Sort.RELEVANCE)).subList(0, 3)).containsExactly("Tomato", "Strawberry", "Carrot");
    }

    @Test
    void pagesThroughResults() {
        ProductSearchResult first = search.search(new Query(null, null, null, null, false, Sort.NAME, 0, 2));
        ProductSearchResult last = search.search(new Query(null, null, null, null, false, Sort.NAME, 2, 2));
        assertThat(first.totalItems()).isEqualTo(5);
        assertThat(first.totalPages()).isEqualTo(3);
        assertThat(names(first)).containsExactly("Carrot", "Carrot Cake Mix");
        assertThat(names(last)).containsExactly("Baby Carrot"); // sold out, so last
    }

    @Test
    void suggestsTheClosestNameForATypo() {
        ProductSearchResult result = run("carot", null, Sort.RELEVANCE);
        assertThat(result.items()).isEmpty();
        assertThat(result.didYouMean()).isEqualTo("carrot");
        assertThat(run("xylophone", null, Sort.RELEVANCE).didYouMean()).isNull();
    }

    @Test
    void unknownSortFallsBackToRelevance() {
        assertThat(Sort.from("price-asc")).isEqualTo(Sort.PRICE_ASC);
        assertThat(Sort.from("bogus")).isEqualTo(Sort.RELEVANCE);
        assertThat(Sort.from(null)).isEqualTo(Sort.RELEVANCE);
    }

    @Test
    void editDistanceCountsSingleLetterChanges() {
        assertThat(ProductSearchService.editDistance("carot", "carrot")).isEqualTo(1);
        assertThat(ProductSearchService.editDistance("tomato", "potato")).isEqualTo(2);
        assertThat(ProductSearchService.editDistance("", "kiwi")).isEqualTo(4);
    }

    private ProductSearchResult run(String text, String category, Sort sort) {
        return search.search(new Query(text, category, null, null, false, sort, 0, 24));
    }

    private static List<String> names(ProductSearchResult result) {
        return result.items().stream().map(CatalogProduct::name).toList();
    }

    private static CatalogProduct product(long id, String name, String category, String description,
                                          String price, String offPercent, int stock) {
        return new CatalogProduct(id, name, description, "500 g", null, category, new BigDecimal(price), null,
                offPercent == null ? null : new BigDecimal(offPercent), stock, stock > 0);
    }
}
