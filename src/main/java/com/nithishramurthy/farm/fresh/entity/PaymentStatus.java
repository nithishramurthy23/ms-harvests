package com.nithishramurthy.farm.fresh.entity;

/**
 * Where the money for an order is. Kept separate from OrderStatus, because
 * "has it been paid" and "has it been shipped" change independently.
 *
 * <pre>
 * Cash on delivery: PAY_ON_DELIVERY -> PAID (when delivered)
 * Online:           AWAITING_PAYMENT -> PAID -> REFUND_PENDING -> REFUNDED (if cancelled)
 *                   AWAITING_PAYMENT -> UNPAID (customer gave up, or 30 minutes passed)
 * </pre>
 */
public enum PaymentStatus {
    PAY_ON_DELIVERY,
    AWAITING_PAYMENT,
    PAID,
    UNPAID,
    REFUND_PENDING,
    REFUNDED
}
