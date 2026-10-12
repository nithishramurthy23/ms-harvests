package com.nithishramurthy.farm.fresh.repository;

import com.nithishramurthy.farm.fresh.entity.Order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Database access for orders. */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
      List<Order> findByUserId(Long userId);
}