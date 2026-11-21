package com.nithishramurthy.farm.fresh.payment;

/** What the browser needs to open Razorpay's payment window for one order. */
public record PaymentStart(String keyId, String gatewayOrderId, long amountPaise, String currency) {
}
