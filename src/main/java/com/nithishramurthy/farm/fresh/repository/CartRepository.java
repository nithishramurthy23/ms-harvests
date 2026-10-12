package com.nithishramurthy.farm.fresh.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nithishramurthy.farm.fresh.entity.Cart;

/** Database access for carts. */
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);
}
