package com.nithishramurthy.farm.fresh.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nithishramurthy.farm.fresh.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {}
