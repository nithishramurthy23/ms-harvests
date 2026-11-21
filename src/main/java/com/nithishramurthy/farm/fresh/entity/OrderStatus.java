package com.nithishramurthy.farm.fresh.entity;

import java.util.Set;

/**
 * Lifecycle of an order:
 * PENDING -> PROCESSING -> SHIPPED -> DELIVERED, and cancellable until it ships.
 */
public enum OrderStatus {
    PENDING,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    /** Whether an order in this status may move to {@code next}. */
    public boolean canMoveTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> Set.of(PROCESSING, CANCELLED).contains(next);
            case PROCESSING -> Set.of(SHIPPED, CANCELLED).contains(next);
            case SHIPPED -> next == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}

