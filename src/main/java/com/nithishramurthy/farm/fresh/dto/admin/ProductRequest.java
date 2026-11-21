package com.nithishramurthy.farm.fresh.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Body for creating or editing a product. {@code initialStock} is only used
 * when creating; change stock later with the stock endpoint so it's logged.
 */
public record ProductRequest(
        @NotBlank(message = "Name is required") @Size(max = 100) String name,
        @Size(max = 2000) String description,
        @Size(max = 50) String weight,
        @Size(max = 255) String imageUrl,
        @NotNull(message = "Category is required") Long categoryId,
        @NotNull(message = "Price is required") @DecimalMin(value = "0.01", message = "Price must be more than 0") BigDecimal price,
        @Min(value = 0, message = "Stock can't be negative") Integer initialStock) {
}
