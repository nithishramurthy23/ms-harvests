package com.nithishramurthy.farm.fresh.dto.admin;

import com.nithishramurthy.farm.fresh.entity.OrderStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * The owner's overview.
 *
 * @param ordersByStatus  how many orders are in each status
 * @param deliveredRevenue total of delivered orders (cash collected)
 * @param openRevenue     total of orders placed but not yet delivered or cancelled
 * @param lowStock        visible products with {@code lowStockThreshold} units or fewer
 */
public record SalesSummary(
        Map<OrderStatus, Long> ordersByStatus,
        BigDecimal deliveredRevenue,
        BigDecimal openRevenue,
        long customers,
        int lowStockThreshold,
        List<AdminProduct> lowStock) {
}
