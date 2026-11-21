package com.nithishramurthy.farm.fresh.notification;

/**
 * "Tell the customer about this order." Published by the code that changes the
 * order; OrderEmailListener sends the email once that change is saved.
 */
public record OrderEmailEvent(Long orderId, OrderEmailType type) {
}
