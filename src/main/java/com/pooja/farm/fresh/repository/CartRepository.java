package com.pooja.farm.fresh.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pooja.farm.fresh.entity.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);
}
