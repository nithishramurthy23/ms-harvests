package com.nithishramurthy.farm.fresh.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nithishramurthy.farm.fresh.entity.CartItem;

/** Database access for cart items. */
public interface CartItemRepository extends JpaRepository<CartItem, Long> {}
