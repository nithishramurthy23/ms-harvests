package com.nithishramurthy.farm.fresh.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * One page of search results, plus what the filter bar needs.
 *
 * @param categoryCounts how many results each aisle would have with the other
 *                       filters applied, so the aisle chips can show "Fruits (4)"
 * @param lowestPrice    cheapest and dearest match, for the price filter hints
 * @param didYouMean     a close product name when nothing matched, e.g. "carrot" for "carot"
 */
public record ProductSearchResult(
        List<CatalogProduct> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        Map<String, Long> categoryCounts,
        BigDecimal lowestPrice,
        BigDecimal highestPrice,
        String didYouMean) {
}
