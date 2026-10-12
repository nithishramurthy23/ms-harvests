package com.nithishramurthy.farm.fresh.dto;

import java.math.BigDecimal;

import com.nithishramurthy.farm.fresh.entity.OrderItem;

public class OrderItemDTO {
    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal productPrice;
    private boolean discountApplied;
    private String productWeight;
    private String productImageUrl;

    public OrderItemDTO() {}

    public OrderItemDTO(OrderItem item) {
        this.id = item.getId();
        this.orderId = item.getOrder().getId();
        this.productId = item.getProduct() != null ? item.getProduct().getId() : null;
        this.productName = item.getProductName();
        this.quantity = item.getQuantity();
        this.productPrice = item.getProductPrice();
        this.discountApplied = item.isDiscountApplied();
        this.productWeight = item.getProductWeight();
        this.productImageUrl = item.getProductImageUrl();
    }



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(BigDecimal productPrice) {
        this.productPrice = productPrice;
    }

    public boolean isDiscountApplied() {
        return discountApplied;
    }

    public void setDiscountApplied(boolean discountApplied) {
        this.discountApplied = discountApplied;
    }

    public String getProductWeight() {
        return productWeight;
    }

    public void setProductWeight(String productWeight) {
        this.productWeight = productWeight;
    }

    public String getProductImageUrl() {
        return productImageUrl;
    }

    public void setProductImageUrl(String productImageUrl) {
        this.productImageUrl = productImageUrl;
    }

    @Override
    public String toString() {
        return "OrderItemDTO [id=" + id + ", orderId=" + orderId + ", productId=" + productId + 
               ", productName=" + productName + ", quantity=" + quantity + ", productPrice=" + productPrice +
               ", discountApplied=" + discountApplied + ", productWeight=" + productWeight + 
               ", productImageUrl=" + productImageUrl + "]";
    }
}
