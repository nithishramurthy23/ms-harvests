package com.nithishramurthy.farm.fresh.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderResponse {
    private Long orderId;
    private String status; // e.g., "Pending", "Confirmed", "Shipped"
    private BigDecimal totalAmount;
    private LocalDateTime orderDate;
    private String message;

    // Constructors, Getters, Setters
    public OrderResponse() {}

    public OrderResponse(Long orderId, String status, BigDecimal totalAmount, LocalDateTime orderDate, String message) {
        this.orderId = orderId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
        this.message = message;
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