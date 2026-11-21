package com.nithishramurthy.farm.fresh.notification;

/** The emails a customer gets about an order, in the order they usually arrive. */
public enum OrderEmailType {
    /** Cash on delivery: as soon as it's placed. Online: once the payment arrives. */
    CONFIRMED,
    /** The order left the shop. */
    SHIPPED,
    DELIVERED,
    /** By the customer, an admin, or because an online order wasn't paid in time. */
    CANCELLED,
    /** The money for a cancelled online order has been sent back. */
    REFUNDED
}
