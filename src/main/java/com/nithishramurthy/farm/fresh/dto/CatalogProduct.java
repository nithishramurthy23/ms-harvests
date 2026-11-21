package com.nithishramurthy.farm.fresh.dto;

import java.math.BigDecimal;

/**
 * A product as the shop shows it: today's price already worked out on the
 * server, plus how many are left.
 *
 * @param category   category slug, for example "fruits"
 * @param price      what the customer pays today
 * @param mrp        the full price, only set when today's price is lower
 * @param offPercent today's discount, only set when there is one
 * @param stock      units left
 * @param inStock    true when at least one unit is left
 */
public record CatalogProduct(
        Long id,
        String name,
        String description,
        String weight,
        String image,
        String category,
        BigDecimal price,
        BigDecimal mrp,
        BigDecimal offPercent,
        int stock,
        boolean inStock) {
}
