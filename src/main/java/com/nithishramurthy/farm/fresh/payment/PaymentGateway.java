package com.nithishramurthy.farm.fresh.payment;

/**
 * The few things the shop needs from a payment provider. RazorpayGateway is the
 * real one; tests replace it with a mock, so no test talks to Razorpay.
 */
public interface PaymentGateway {

    /** True when keys are configured. Without them the shop only offers cash on delivery. */
    boolean isEnabled();

    /** Public key the browser needs to open the payment window. Safe to share. */
    String publicKey();

    /**
     * Tells the provider how much to collect for one of our orders.
     *
     * @param amountPaise amount in the smallest unit (₹149.00 is 14900)
     * @param receipt     our own reference, e.g. "order_41"
     * @return the provider's order id (order_...)
     */
    String createOrder(long amountPaise, String receipt);

    /** Checks the signature the browser sends back after a successful payment. */
    boolean isValidPaymentSignature(String gatewayOrderId, String paymentId, String signature);

    /** Checks that a webhook really came from the provider. */
    boolean isValidWebhookSignature(String rawBody, String signature);

    /** Refunds the whole payment. */
    void refund(String paymentId, long amountPaise);
}
