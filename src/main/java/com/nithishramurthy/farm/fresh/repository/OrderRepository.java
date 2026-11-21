package com.nithishramurthy.farm.fresh.repository;

import com.nithishramurthy.farm.fresh.entity.OrderStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

import com.nithishramurthy.farm.fresh.entity.Order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Database access for orders. */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
      List<Order> findByUserId(Long userId);

      List<Order> findAllByOrderByOrderDateDescIdDesc();

      List<Order> findByStatusOrderByOrderDateDescIdDesc(OrderStatus status);

      long countByStatus(OrderStatus status);

      @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status")
      BigDecimal sumTotalByStatus(@Param("status") OrderStatus status);
}