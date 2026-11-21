package com.nithishramurthy.farm.fresh.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nithishramurthy.farm.fresh.entity.Order;
import com.nithishramurthy.farm.fresh.entity.PaymentStatus;
import com.nithishramurthy.farm.fresh.exception.ResourceNotFoundException;
import com.nithishramurthy.farm.fresh.repository.OrderRepository;
import com.nithishramurthy.farm.fresh.service.MailService;
import com.nithishramurthy.farm.fresh.service.OrderFulfilmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Online payments, from "please pay" to "paid" (or "refunded").
 *
 * The browser and Razorpay's webhook can both report the same payment, in
 * any order, possibly at the same time. Every state change therefore goes
 * through {@link OrderRepository#movePayment}, a conditional UPDATE that only
 * one of them can win. That is the same trick checkout uses for stock.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentGateway gateway;
    private final OrderRepository orderRepository;
    private final OrderFulfilmentService fulfilment;
    private final MailService mailService;
    private final ApplicationEventPublisher events;
    private final ObjectMapper objectMapper;
    private final Duration payWithin;

    public PaymentService(PaymentGateway gateway, OrderRepository orderRepository, OrderFulfilmentService fulfilment,
                          MailService mailService, ApplicationEventPublisher events, ObjectMapper objectMapper,
                          @Value("${app.payments.pay-within-minutes:30}") long payWithinMinutes) {
        this.gateway = gateway;
        this.orderRepository = orderRepository;
        this.fulfilment = fulfilment;
        this.mailService = mailService;
        this.events = events;
        this.objectMapper = objectMapper;
        this.payWithin = Duration.ofMinutes(payWithinMinutes);
    }

    public boolean isOnlineEnabled() {
        return gateway.isEnabled();
    }

    public String publicKey() {
        return gateway.publicKey();
    }

    /**
     * Asks Razorpay for a payment order. Called after the order (and its stock)
     * is saved, never inside that transaction: holding row locks on products
     * while waiting on another company's API would slow every other checkout.
     * If Razorpay can't be reached, the order is cancelled and its stock released.
     */
    public PaymentStart start(Order order) {
        long amount = toPaise(order.getTotalAmount());
        try {
            String gatewayOrderId = gateway.createOrder(amount, "order_" + order.getId());
            fulfilment.attachGatewayOrder(order.getId(), gatewayOrderId);
            return new PaymentStart(gateway.publicKey(), gatewayOrderId, amount, "INR");
        } catch (RuntimeException e) {
            log.error("Couldn't start payment for order {}", order.getId(), e);
            fulfilment.cancelUnpaid(order.getId(), "payment-setup-failed");
            throw new IllegalArgumentException(
                    "Online payment isn't available right now. Please try again, or pick cash on delivery.");
        }
    }

    /** Lets a customer open the payment window again for an order they haven't paid yet. */
    @Transactional(readOnly = true)
    public PaymentStart resume(Long orderId, Long userId) {
        Order order = ownOrder(orderId, userId);
        if (order.getPaymentStatus() != PaymentStatus.AWAITING_PAYMENT || order.getGatewayOrderId() == null) {
            throw new IllegalArgumentException("This order isn't waiting for payment.");
        }
        return new PaymentStart(gateway.publicKey(), order.getGatewayOrderId(), toPaise(order.getTotalAmount()), "INR");
    }

    /**
     * The browser reports a successful payment. The signature proves the ids
     * came from Razorpay and weren't typed in by the customer.
     */
    @Transactional
    public PaymentStatus confirmFromCheckout(Long orderId, Long userId, String gatewayOrderId,
                                             String paymentId, String signature) {
        Order order = ownOrder(orderId, userId);
        if (order.getGatewayOrderId() == null || !order.getGatewayOrderId().equals(gatewayOrderId)
                || !gateway.isValidPaymentSignature(gatewayOrderId, paymentId, signature)) {
            throw new IllegalArgumentException("We couldn't verify this payment.");
        }
        return markPaid(order.getId(), paymentId);
    }

    /**
     * Razorpay calls this for every payment event. It's the safety net for when
     * the customer closes the tab right after paying, before the browser reports it.
     *
     * @return false if the signature is wrong
     */
    @Transactional
    public boolean handleWebhook(String rawBody, String signature) {
        if (!gateway.isValidWebhookSignature(rawBody, signature)) {
            return false;
        }
        JsonNode event;
        try {
            event = objectMapper.readTree(rawBody);
        } catch (IOException e) {
            return false;
        }
        String type = event.path("event").asText();
        if (!type.equals("payment.captured") && !type.equals("order.paid")) {
            return true; // other events (payment.failed, ...) need nothing: the customer can try again
        }
        JsonNode payment = event.path("payload").path("payment").path("entity");
        String gatewayOrderId = payment.path("order_id").asText();
        String paymentId = payment.path("id").asText();
        long amount = payment.path("amount").asLong();

        orderRepository.findByGatewayOrderId(gatewayOrderId).ifPresentOrElse(order -> {
            if (amount != toPaise(order.getTotalAmount())) {
                log.error("Payment {} for order {} was {} paise, expected {}", paymentId, order.getId(), amount,
                        toPaise(order.getTotalAmount()));
                return;
            }
            markPaid(order.getId(), paymentId);
        }, () -> log.warn("Webhook for unknown Razorpay order {}", gatewayOrderId));
        return true;
    }

    /**
     * Records a payment exactly once. If the order expired or was cancelled just
     * before the money arrived, the customer has paid for nothing, so it is
     * refunded straight away.
     */
    @Transactional
    public PaymentStatus markPaid(Long orderId, String paymentId) {
        if (orderRepository.movePayment(orderId, PaymentStatus.AWAITING_PAYMENT, PaymentStatus.PAID, paymentId) == 1) {
            Order order = orderRepository.findById(orderId).orElseThrow();
            sendConfirmation(order);
            return PaymentStatus.PAID;
        }
        if (orderRepository.movePayment(orderId, PaymentStatus.UNPAID, PaymentStatus.REFUND_PENDING, paymentId) == 1) {
            log.warn("Payment {} arrived after order {} was cancelled; refunding it", paymentId, orderId);
            events.publishEvent(new OrderCancelledEvent(orderId));
            return PaymentStatus.REFUND_PENDING;
        }
        // Already applied (browser and webhook both reported it). Nothing to do.
        return orderRepository.findPaymentStatus(orderId);
    }

    /** Refunds a cancelled order once the cancellation is saved. */
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderCancelled(OrderCancelledEvent event) {
        refund(event.orderId());
    }

    /**
     * Sends the refund to Razorpay. The order is marked REFUNDED first, so two
     * callers (the automatic refund and an admin pressing "Retry refund") can't
     * both send money back. If Razorpay fails, it goes back to REFUND_PENDING.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentStatus refund(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (order.getPaymentStatus() != PaymentStatus.REFUND_PENDING) {
            return order.getPaymentStatus();
        }
        if (orderRepository.movePayment(orderId, PaymentStatus.REFUND_PENDING, PaymentStatus.REFUNDED, null) == 0) {
            return PaymentStatus.REFUNDED; // someone else is refunding it right now
        }
        try {
            gateway.refund(order.getGatewayPaymentId(), toPaise(order.getTotalAmount()));
            log.info("Refunded order {} (payment {})", orderId, order.getGatewayPaymentId());
            return PaymentStatus.REFUNDED;
        } catch (RuntimeException e) {
            log.error("Refund for order {} failed; an admin can retry it", orderId, e);
            orderRepository.movePayment(orderId, PaymentStatus.REFUNDED, PaymentStatus.REFUND_PENDING, null);
            return PaymentStatus.REFUND_PENDING;
        }
    }

    /**
     * Orders that were never paid hold stock that other customers could buy.
     * Every minute, cancel the ones older than {@code pay-within-minutes} and put
     * their stock back.
     */
    @Scheduled(fixedDelayString = "${app.payments.expiry-check-ms:60000}", initialDelay = 60_000)
    public void expireUnpaidOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minus(payWithin);
        for (Order order : orderRepository.findByPaymentStatusAndOrderDateBefore(PaymentStatus.AWAITING_PAYMENT, cutoff)) {
            try {
                fulfilment.cancelUnpaid(order.getId(), "payment-timeout");
            } catch (RuntimeException e) {
                log.error("Couldn't expire unpaid order {}", order.getId(), e);
            }
        }
    }

    private void sendConfirmation(Order order) {
        try {
            mailService.sendOrderConfirmationEmail(order, order.getUser().getEmail(), order.getUser().getName());
        } catch (RuntimeException e) {
            log.warn("Order {} is paid but the confirmation email failed", order.getId(), e);
        }
    }

    private Order ownOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found: " + orderId); // don't reveal other people's orders
        }
        return order;
    }

    static long toPaise(BigDecimal rupees) {
        return rupees.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }
}
