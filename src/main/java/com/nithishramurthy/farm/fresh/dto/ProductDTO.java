package com.nithishramurthy.farm.fresh.dto;

import java.math.BigDecimal;

import com.nithishramurthy.farm.fresh.entity.Product;

public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private String weight;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String imageUrl;
    private Long categoryId;
    private String categoryName;

    public ProductDTO(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.weight = product.getWeight();
        this.price = product.getPrice();
        this.originalPrice = product.getOriginalPrice();
        this.imageUrl = product.getImageUrl();
        this.categoryId = product.getCategory().getId();
        this.categoryName = product.getCategory().getName();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    @Override
    public String toString() {
        return "ProductDTO [id=" + id + ", name=" + name + ", description=" + description + ", weight=" + weight
                + ", price=" + price + ", originalPrice=" + originalPrice + ", imageUrl=" + imageUrl + ", categoryId="
                + categoryId + ", categoryName=" + categoryName + "]";
    }

    
}

