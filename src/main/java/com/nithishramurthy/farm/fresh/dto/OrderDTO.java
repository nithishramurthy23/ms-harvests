package com.nithishramurthy.farm.fresh.dto;

import com.nithishramurthy.farm.fresh.entity.Order;
import com.nithishramurthy.farm.fresh.entity.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class OrderDTO {

    private Long id;
    private Long userId;
    private String shippingAddress;
    private String paymentMethod;
    private String status;
    private LocalDateTime orderDate;
    private BigDecimal totalAmount;
    private List<OrderItemDTO> orderItems;

    // Constructor mapping from entity
    public OrderDTO(Order order) {
        this.id = order.getId();
        this.userId = order.getUser() != null ? order.getUser().getId() : null;
        this.shippingAddress = order.getShippingAddress();
        this.paymentMethod = order.getPaymentMethod();
        this.status = order.getStatus() != null ? order.getStatus().name() : null;
        this.orderDate = order.getOrderDate();
        this.totalAmount = order.getTotalAmount();
        this.orderItems = order.getOrderItems() != null
                ? order.getOrderItems().stream().map(OrderItemDTO::new).collect(Collectors.toList())
                : null;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<OrderItemDTO> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItemDTO> orderItems) {
        this.orderItems = orderItems;
    }

    @Override
    public String toString() {
        return "OrderDTO{" +
                "id=" + id +
                ", userId=" + userId +
                ", shippingAddress='" + shippingAddress + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", status='" + status + '\'' +
                ", orderDate=" + orderDate +
                ", totalAmount=" + totalAmount +
                ", orderItems=" + orderItems +
                '}';
    }
}
