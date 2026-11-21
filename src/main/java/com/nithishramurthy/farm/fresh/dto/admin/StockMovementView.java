package com.nithishramurthy.farm.fresh.dto.admin;

import com.nithishramurthy.farm.fresh.entity.StockMovement;

import java.time.Instant;

/** One line of a product's stock history. */
public record StockMovementView(Long id, int change, int stockAfter, String reason, String changedBy, Instant changedAt) {

    public static StockMovementView from(StockMovement m) {
        return new StockMovementView(m.getId(), m.getChange(), m.getStockAfter(), m.getReason(), m.getChangedBy(), m.getChangedAt());
    }
}
