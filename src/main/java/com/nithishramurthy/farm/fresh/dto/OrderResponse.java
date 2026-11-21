package com.nithishramurthy.farm.fresh.dto;

import com.nithishramurthy.farm.fresh.payment.PaymentStart;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Returned after an order is placed: id, status, total and time. */
public class OrderResponse {
    private Long orderId;
    private String status; // e.g., "Pending", "Confirmed", "Shipped"
    private BigDecimal totalAmount;
    private LocalDateTime orderDate;
    private String message;
    private String paymentStatus;
    /** Set only for online payments that still need paying: what the browser needs to open Razorpay. */
    private PaymentStart payment;

    // Constructors, Getters, Setters
    public OrderResponse() {}

    public OrderResponse(Long orderId, String status, BigDecimal totalAmount, LocalDateTime orderDate, String message) {
        this.orderId = orderId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
        this.message = message;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public PaymentStart getPayment() {
        return payment;
    }

    public void setPayment(PaymentStart payment) {
        this.payment = payment;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}