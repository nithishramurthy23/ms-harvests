package com.nithishramurthy.farm.fresh.repository;

import com.nithishramurthy.farm.fresh.entity.Order;
import com.nithishramurthy.farm.fresh.entity.OrderStatus;
import com.nithishramurthy.farm.fresh.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Database access for orders. */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
      List<Order> findByUserId(Long userId);

      List<Order> findAllByOrderByOrderDateDescIdDesc();

      List<Order> findByStatusOrderByOrderDateDescIdDesc(OrderStatus status);

      long countByStatus(OrderStatus status);

      Optional<Order> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

      Optional<Order> findByGatewayOrderId(String gatewayOrderId);

      List<Order> findByPaymentStatusAndOrderDateBefore(PaymentStatus paymentStatus, LocalDateTime cutoff);

      /**
       * Moves the payment on only if it is still in the expected state. Returns 0 if
       * someone else got there first (the webhook and the browser both report the same
       * payment, or the order expired a moment earlier), so each payment is applied once.
       */
      @Modifying(flushAutomatically = true)
      @Query("UPDATE Order o SET o.paymentStatus = :next, o.gatewayPaymentId = COALESCE(:paymentId, o.gatewayPaymentId) "
              + "WHERE o.id = :id AND o.paymentStatus = :expected")
      int movePayment(@Param("id") Long id, @Param("expected") PaymentStatus expected,
                      @Param("next") PaymentStatus next, @Param("paymentId") String paymentId);

      @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status")
      BigDecimal sumTotalByStatus(@Param("status") OrderStatus status);

      /** Reads the payment status straight from the database, skipping any cached copy of the order. */
      @Query("SELECT o.paymentStatus FROM Order o WHERE o.id = :id")
      PaymentStatus findPaymentStatus(@Param("id") Long id);
}
