package com.nithishramurthy.farm.fresh.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nithishramurthy.farm.fresh.entity.Product;

/** Database access for products, including the atomic stock update used at checkout. */
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

 /**
  * Adds (positive) or removes (negative) stock in one statement, refusing to go
  * below zero. Returns 1 if it changed, 0 if it would have gone negative.
  */
 @Modifying(flushAutomatically = true, clearAutomatically = true)
 @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity + :change "
      + "WHERE p.id = :productId AND p.stockQuantity + :change >= 0")
 int adjustStock(@Param("productId") Long productId, @Param("change") int change);

 /** Products customers can see. */
 List<Product> findByActiveTrue();

 /** Visible products with this much stock or less, lowest first. */
 List<Product> findByActiveTrueAndStockQuantityLessThanEqualOrderByStockQuantityAsc(int threshold);
}
