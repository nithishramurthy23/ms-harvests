package com.nithishramurthy.farm.fresh.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nithishramurthy.farm.fresh.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
 List<Product> findByCategoryId(Long categoryId);

 /**
  * Atomically takes stock in the database, so two orders running at the same
  * time (even on different app instances) can never oversell.
  * Returns 1 if the stock was reserved, 0 if there wasn't enough.
  */
 @Modifying(flushAutomatically = true)
 @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity "
      + "WHERE p.id = :productId AND p.stockQuantity >= :quantity")
 int decrementStock(@Param("productId") Long productId, @Param("quantity") int quantity);
}
