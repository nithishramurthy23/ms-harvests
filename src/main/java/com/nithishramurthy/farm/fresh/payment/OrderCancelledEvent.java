package com.nithishramurthy.farm.fresh.payment;

/** Published when an order is cancelled, so a paid order can be refunded after the cancel is saved. */
public record OrderCancelledEvent(Long orderId) {
}
