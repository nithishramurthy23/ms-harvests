package com.nithishramurthy.farm.fresh.repository;

import com.nithishramurthy.farm.fresh.entity.OrderItem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/** Database access for order lines. */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Units sold per product since a date, leaving out cancelled orders. Used to
     * sort the shop by "popular". Each row is [productId, units].
     */
    @Query("SELECT i.product.id, SUM(i.quantity) FROM OrderItem i "
            + "WHERE i.order.orderDate >= :since AND i.order.status <> com.nithishramurthy.farm.fresh.entity.OrderStatus.CANCELLED "
            + "GROUP BY i.product.id")
    List<Object[]> unitsSoldSince(@Param("since") LocalDateTime since);
}
