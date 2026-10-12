package com.nithishramurthy.farm.fresh.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.nithishramurthy.farm.fresh.entity.Cart;

public class CartDTO {
    private Long id;
    private Long userId;
    private LocalDateTime createdAt;
    private List<CartItemDTO> items;

    public CartDTO(Cart cart) {
        this.id = cart.getId();
        this.userId = cart.getUser().getId();
        this.createdAt = cart.getCreatedAt();
        this.items = cart.getCartItems().stream()
            .map(CartItemDTO::new).collect(Collectors.toList());
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<CartItemDTO> getItems() {
        return items;
    }

    public void setItems(List<CartItemDTO> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "CartDTO [id=" + id + ", userId=" + userId + ", createdAt=" + createdAt + ", items=" + items + "]";
    }

    
}

