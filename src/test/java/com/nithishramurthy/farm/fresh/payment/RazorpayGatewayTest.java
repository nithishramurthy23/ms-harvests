package com.nithishramurthy.farm.fresh.payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Signature checks, against values worked out separately with Python's hmac module. */
class RazorpayGatewayTest {

    private final RazorpayGateway gateway =
            new RazorpayGateway("rzp_test_key", "test_secret", "hook_secret", "http://localhost:0");

    @Test
    void acceptsTheSignatureRazorpaySendsAfterPayment() {
        String signature = "d9bddb3bac5f9d4360c902fc4c048990e896202b9e141c0ca59432b9d97ce141";
        assertThat(gateway.isValidPaymentSignature("order_RZP1", "pay_1", signature)).isTrue();
    }

    @Test
    void rejectsASignatureForADifferentPayment() {
        String signature = "d9bddb3bac5f9d4360c902fc4c048990e896202b9e141c0ca59432b9d97ce141";
        assertThat(gateway.isValidPaymentSignature("order_RZP1", "pay_2", signature)).isFalse();
        assertThat(gateway.isValidPaymentSignature("order_RZP1", "pay_1", null)).isFalse();
    }

    @Test
    void checksWebhooksWithTheirOwnSecret() {
        String body = "{\"event\":\"payment.captured\"}";
        assertThat(gateway.isValidWebhookSignature(body,
                "d4e67baa44ce74e853cfe47567a9e8fd04e1e43a407810408215b1b77012c0ac")).isTrue();
        assertThat(gateway.isValidWebhookSignature(body + " ",
                "d4e67baa44ce74e853cfe47567a9e8fd04e1e43a407810408215b1b77012c0ac")).isFalse();
    }

    @Test
    void ignoresWebhooksWhenNoSecretIsSet() {
        RazorpayGateway noSecret = new RazorpayGateway("rzp_test_key", "test_secret", "", "http://localhost:0");
        assertThat(noSecret.isValidWebhookSignature("{}", "anything")).isFalse();
    }

    @Test
    void isOffWithoutKeys() {
        assertThat(new RazorpayGateway("", "", "", "http://localhost:0").isEnabled()).isFalse();
        assertThat(gateway.isEnabled()).isTrue();
    }

    @Test
    void convertsRupeesToPaise() {
        assertThat(PaymentService.toPaise(new BigDecimal("149.00"))).isEqualTo(14900);
        assertThat(PaymentService.toPaise(new BigDecimal("298.5"))).isEqualTo(29850);
    }
}
