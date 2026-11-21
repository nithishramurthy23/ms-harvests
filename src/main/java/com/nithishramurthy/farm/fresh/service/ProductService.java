package com.nithishramurthy.farm.fresh.service;

import com.nithishramurthy.farm.fresh.dto.CatalogProduct;
import com.nithishramurthy.farm.fresh.dto.TodayOffer;
import com.nithishramurthy.farm.fresh.entity.DiscountRule;
import com.nithishramurthy.farm.fresh.entity.Product;
import com.nithishramurthy.farm.fresh.repository.DiscountRuleRepository;
import com.nithishramurthy.farm.fresh.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Product pricing, including today's category discounts, and the catalogue the shop shows. */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final DiscountRuleRepository discountRuleRepository;

    public ProductService(ProductRepository productRepository,
                          DiscountRuleRepository discountRuleRepository) {
        this.productRepository = productRepository;
        this.discountRuleRepository = discountRuleRepository;
    }

    /**
     * Price a customer pays for this product today: the original price minus
     * today's active category discount, or the product's normal price if no
     * discount applies. Does not modify the product.
     */
    public BigDecimal getCurrentPrice(Product product) {
        return priceWith(product, todaysRules());
    }

    /** Every visible product with today's price and stock, ordered by id. */
    @Transactional(readOnly = true)
    public List<CatalogProduct> listCatalog() {
        List<DiscountRule> rules = todaysRules();
        return productRepository.findByActiveTrue().stream()
                .sorted(Comparator.comparing(Product::getId))
                .map(product -> toCatalogProduct(product, rules))
                .toList();
    }

    /** One product with today's price and stock. */
    @Transactional(readOnly = true)
    public Optional<CatalogProduct> findCatalogProduct(Long id) {
        return productRepository.findById(id)
                .filter(Product::isActive)
                .map(product -> toCatalogProduct(product, todaysRules()));
    }

    /** Today's category offer, if there is one. */
    @Transactional(readOnly = true)
    public Optional<TodayOffer> todayOffer() {
        return todaysRules().stream()
                .filter(rule -> rule.getCategory() != null)
                .findFirst()
                .map(rule -> new TodayOffer(rule.getCategory().slug(), rule.getDiscountPercent()));
    }

    private List<DiscountRule> todaysRules() {
        return discountRuleRepository.findByDayOfWeekAndActive(LocalDate.now().getDayOfWeek(), true);
    }

    private static BigDecimal priceWith(Product product, List<DiscountRule> rules) {
        if (product.getCategory() != null) {
            for (DiscountRule rule : rules) {
                if (rule.getCategory().getId().equals(product.getCategory().getId())) {
                    BigDecimal remaining = BigDecimal.valueOf(100).subtract(rule.getDiscountPercent());
                    return product.getOriginalPrice()
                            .multiply(remaining)
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP); // whole rupees
                }
            }
        }
        return product.getPrice();
    }

    private static CatalogProduct toCatalogProduct(Product product, List<DiscountRule> rules) {
        BigDecimal price = priceWith(product, rules);
        BigDecimal full = product.getOriginalPrice();
        boolean discounted = full != null && price.compareTo(full) < 0;
        BigDecimal offPercent = discounted
                ? full.subtract(price).multiply(BigDecimal.valueOf(100)).divide(full, 0, RoundingMode.HALF_UP)
                : null;
        int stock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();

        return new CatalogProduct(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getWeight(),
                product.getImageUrl(),
                product.getCategory() == null ? null : product.getCategory().slug(),
                price,
                discounted ? full : null,
                offPercent,
                stock,
                stock > 0);
    }
}
