package com.nithishramurthy.farm.fresh.dto;

import java.math.BigDecimal;

import com.nithishramurthy.farm.fresh.entity.CartItem;

public class CartItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal discountedPrice;
    private BigDecimal originalPrice;
    private boolean discountApplied;

    public CartItemDTO(CartItem item) {
        this.id = item.getId();
        this.productId = item.getProduct().getId();
        this.productName = item.getProduct().getName();
        this.quantity = item.getQuantity();
        this.discountedPrice = item.getDiscountedPrice();
        this.originalPrice = item.getOriginalPrice();
        this.discountApplied = item.isDiscountApplied();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice(BigDecimal discountedPrice) {
        this.discountedPrice = discountedPrice;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public boolean isDiscountApplied() {
        return discountApplied;
    }

    public void setDiscountApplied(boolean discountApplied) {
        this.discountApplied = discountApplied;
    }

    @Override
    public String toString() {
        return "CartItemDTO [id=" + id + ", productId=" + productId + ", productName=" + productName + ", quantity="
                + quantity + ", discountedPrice=" + discountedPrice + ", originalPrice=" + originalPrice
                + ", discountApplied=" + discountApplied + "]";
    }

   
}

