package com.pooja.farm.fresh.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pooja.farm.fresh.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {}
