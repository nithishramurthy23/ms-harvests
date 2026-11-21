package com.nithishramurthy.farm.fresh.repository;

import com.nithishramurthy.farm.fresh.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** History of stock changes per product. */
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByProductIdOrderByChangedAtDescIdDesc(Long productId);
}
