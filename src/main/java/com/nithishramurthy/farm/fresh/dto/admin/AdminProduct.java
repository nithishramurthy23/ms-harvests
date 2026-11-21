package com.nithishramurthy.farm.fresh.dto.admin;

import com.nithishramurthy.farm.fresh.entity.Product;

import java.math.BigDecimal;

/** A product as admins see it, including hidden ones and the raw stock count. */
public record AdminProduct(
        Long id,
        String name,
        String description,
        String weight,
        String imageUrl,
        Long categoryId,
        String categoryName,
        BigDecimal price,
        int stock,
        boolean active) {

    public static AdminProduct from(Product p) {
        return new AdminProduct(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getWeight(),
                p.getImageUrl(),
                p.getCategory() == null ? null : p.getCategory().getId(),
                p.getCategory() == null ? null : p.getCategory().getName(),
                p.getOriginalPrice(),
                p.getStockQuantity() == null ? 0 : p.getStockQuantity(),
                p.isActive());
    }
}
