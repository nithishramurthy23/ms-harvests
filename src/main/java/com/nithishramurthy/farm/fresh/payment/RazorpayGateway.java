package com.nithishramurthy.farm.fresh.payment;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

/**
 * Razorpay over its REST API (https://razorpay.com/docs/api/). No SDK: we only
 * need three calls and two signature checks, and this keeps them easy to read.
 *
 * Use test keys (rzp_test_...) while developing. Nothing is charged, and
 * Razorpay's test cards and UPI ids always succeed or fail on demand.
 */
@Component
public class RazorpayGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(RazorpayGateway.class);

    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final RestClient http;

    public RazorpayGateway(@Value("${app.payments.razorpay.key-id:}") String keyId,
                           @Value("${app.payments.razorpay.key-secret:}") String keySecret,
                           @Value("${app.payments.razorpay.webhook-secret:}") String webhookSecret,
                           @Value("${app.payments.razorpay.base-url:https://api.razorpay.com/v1}") String baseUrl) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;

        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(5_000);
        timeouts.setReadTimeout(10_000); // never let a slow gateway hold a checkout thread for long
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(timeouts)
                .defaultHeaders(h -> h.setBasicAuth(keyId, keySecret))
                .build();
    }

    @Override
    public boolean isEnabled() {
        return !keyId.isBlank() && !keySecret.isBlank();
    }

    /** The key id (rzp_test_... or rzp_live_...). Public by design; the secret never leaves the server. */
    @Override
    public String publicKey() {
        return keyId;
    }

    /**
     * POST /orders on Razorpay. Razorpay remembers the amount, so the customer can't change
     * it in the payment window. Returns Razorpay's id, e.g. "order_N8x...".
     */
    @Override
    public String createOrder(long amountPaise, String receipt) {
        JsonNode created = http.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amountPaise, "currency", "INR", "receipt", receipt))
                .retrieve()
                .body(JsonNode.class);
        return created.get("id").asText();
    }

    /**
     * After a successful payment Razorpay gives the browser a signature: HMAC-SHA256 of
     * "order_id|payment_id" using our key secret. Only Razorpay and we know that secret,
     * so a matching signature proves the ids weren't made up by the customer.
     */
    @Override
    public boolean isValidPaymentSignature(String gatewayOrderId, String paymentId, String signature) {
        return matches(hmacSha256Hex(gatewayOrderId + "|" + paymentId, keySecret), signature);
    }

    @Override
    public boolean isValidWebhookSignature(String rawBody, String signature) {
        if (webhookSecret.isBlank()) {
            log.warn("Razorpay webhook received but RAZORPAY_WEBHOOK_SECRET is not set; ignoring it");
            return false;
        }
        return matches(hmacSha256Hex(rawBody, webhookSecret), signature);
    }

    /** POST /payments/{id}/refund for the full amount. Throws if Razorpay says no; the caller handles that. */
    @Override
    public void refund(String paymentId, long amountPaise) {
        http.post()
                .uri("/payments/{id}/refund", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amountPaise))
                .retrieve()
                .toBodilessEntity();
    }

    /** HMAC-SHA256 of the text with the secret, as lower-case hex, which is the format Razorpay sends. */
    static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }

    /** Constant-time compare, so the response time doesn't leak how much of a guess was right. */
    private static boolean matches(String expected, String given) {
        if (given == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8));
    }
}
