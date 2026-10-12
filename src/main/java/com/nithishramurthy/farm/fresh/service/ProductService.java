package com.nithishramurthy.farm.fresh.service;


import com.nithishramurthy.farm.fresh.entity.DiscountRule;
import com.nithishramurthy.farm.fresh.entity.Product;
import com.nithishramurthy.farm.fresh.repository.DiscountRuleRepository;
import com.nithishramurthy.farm.fresh.repository.ProductRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

/** Product pricing, including today's category discounts. */
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
        if (product.getCategory() != null) {
            var today = LocalDate.now().getDayOfWeek();
            for (DiscountRule rule : discountRuleRepository.findByDayOfWeekAndActive(today, true)) {
                if (rule.getCategory().getId().equals(product.getCategory().getId())) {
                    BigDecimal remaining = BigDecimal.valueOf(100).subtract(rule.getDiscountPercent());
                    return product.getOriginalPrice()
                            .multiply(remaining)
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP); // whole rupees, same as the frontend
                }
            }
        }
        return product.getPrice();
    }

    /** All products with today's discount applied to their price (not saved). */
    public List<Product> getDiscountedProducts() {
        var currentDay = LocalDate.now().getDayOfWeek();

        List<DiscountRule> rules = discountRuleRepository.findByDayOfWeekAndActive(currentDay, true);

        return productRepository.findAll().stream().map(product -> {
            DiscountRule rule = rules.stream()
                    .filter(r -> r.getCategory().getId().equals(product.getCategory().getId()))
                    .findFirst()
                    .orElse(null);

            if (rule != null) {
                BigDecimal discountMultiplier = BigDecimal.valueOf(1 - (rule.getDiscountPercent().doubleValue() / 100));
                product.setPrice(product.getOriginalPrice().multiply(discountMultiplier));
            }

            return product;
        }).collect(Collectors.toList());
    }
}
