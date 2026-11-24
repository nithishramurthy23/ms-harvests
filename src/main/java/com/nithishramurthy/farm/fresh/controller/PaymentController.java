package com.nithishramurthy.farm.fresh.controller;

import com.nithishramurthy.farm.fresh.details.CustomUserDetails;
import com.nithishramurthy.farm.fresh.entity.PaymentStatus;
import com.nithishramurthy.farm.fresh.exception.ResourceNotFoundException;
import com.nithishramurthy.farm.fresh.payment.PaymentService;
import com.nithishramurthy.farm.fresh.payment.PaymentStart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Online payments. The order itself is still placed with POST /api/orders/place-order
 * (paymentMethod "online"); these endpoints finish the payment.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** Whether checkout should offer online payment, and the public key to open Razorpay with. */
    @GetMapping("/config")
    public Map<String, Object> config() {
        boolean enabled = paymentService.isOnlineEnabled();
        return Map.of("onlineEnabled", enabled, "keyId", enabled ? paymentService.publicKey() : "");
    }

    /** What Razorpay's payment window hands the browser after a successful payment. */
    public record VerifyRequest(@NotNull Long orderId, @NotBlank String razorpayOrderId,
                                @NotBlank String razorpayPaymentId, @NotBlank String razorpaySignature) {
    }

    /** The browser reports a successful payment from the Razorpay window. */
    @PostMapping("/verify")
    public Map<String, Object> verify(@Valid @RequestBody VerifyRequest request,
                                      @AuthenticationPrincipal CustomUserDetails me) {
        PaymentStatus status = paymentService.confirmFromCheckout(request.orderId(), me.getUser().getId(),
                request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature());
        return Map.of("orderId", request.orderId(), "paymentStatus", status);
    }

    /** Opens the payment window again for an order that hasn't been paid yet. */
    @PostMapping("/orders/{orderId}/retry")
    public PaymentStart retry(@PathVariable Long orderId, @AuthenticationPrincipal CustomUserDetails me) {
        return paymentService.resume(orderId, me.getUser().getId());
    }

    /** Razorpay tells us about payments here. Set it up in the Razorpay dashboard (docs/payments.md). */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String rawBody,
                                        @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        return paymentService.handleWebhook(rawBody, signature)
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<Map<String, String>> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Order not found."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}
