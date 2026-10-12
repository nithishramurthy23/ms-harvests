package com.nithishramurthy.farm.fresh.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // Use FetchType.LAZY for better performance
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY) // Correctly maps to Product entity
    @JoinColumn(name = "product_id", nullable = false) // Foreign key to products table
    private Product product; // Reference to the actual product

    // --- DENORMALIZED PRODUCT DETAILS ---
    // These fields capture product info at the time of purchase for historical accuracy
    @Column(nullable = false)
    private String productName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal productPrice; // Price of the product at the time of order

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 50) // e.g., "500g", "1kg"
    private String productWeight;

    @Column(length = 255)
    private String productImageUrl;
    // --- END DENORMALIZED FIELDS ---

    @Column(name = "discount_applied")
    private boolean discountApplied = false;

    // Default constructor for JPA
    public OrderItem() {}

    // Constructor for creating an OrderItem
    public OrderItem(Order order, Product product, String productName, BigDecimal productPrice, Integer quantity, String productWeight, String productImageUrl, boolean discountApplied) {
        this.order = order;
        this.product = product; // Set the Product entity reference
        this.productName = productName;
        this.productPrice = productPrice;
        this.quantity = quantity;
        this.productWeight = productWeight;
        this.productImageUrl = productImageUrl;
        this.discountApplied = discountApplied;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    // Corrected setProduct method (was throwing UnsupportedOperationException)
    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    // New getters/setters for denormalized fields
    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(BigDecimal productPrice) {
        this.productPrice = productPrice;
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    // getPriceAtPurchase() and setPriceAtPurchase() methods are now correctly removed.

    public boolean isDiscountApplied() {
        return discountApplied;
    }

    public void setDiscountApplied(boolean discountApplied) {
        this.discountApplied = discountApplied;
    }

    @Override
    public String toString() {
        return "OrderItem [id=" + id + ", order=" + order + ", product=" + product + ", productName=" + productName + ", productPrice=" + productPrice
                + ", quantity=" + quantity + ", productWeight=" + productWeight + ", productImageUrl=" + productImageUrl
                + ", discountApplied=" + discountApplied + "]";
    }
}