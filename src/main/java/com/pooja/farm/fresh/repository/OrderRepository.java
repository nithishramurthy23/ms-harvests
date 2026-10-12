package com.pooja.farm.fresh.repository;

import com.pooja.farm.fresh.entity.Order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
      List<Order> findByUserId(Long userId);
}