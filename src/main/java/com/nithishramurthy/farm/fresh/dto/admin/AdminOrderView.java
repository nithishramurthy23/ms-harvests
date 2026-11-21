package com.nithishramurthy.farm.fresh.dto.admin;

import com.nithishramurthy.farm.fresh.dto.OrderItemDTO;
import com.nithishramurthy.farm.fresh.entity.Order;
import com.nithishramurthy.farm.fresh.entity.OrderStatus;
import com.nithishramurthy.farm.fresh.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/** An order as admins see it: who placed it, what's in it, and where it can go next. */
public record AdminOrderView(
        Long id,
        String customerName,
        String customerEmail,
        LocalDateTime orderDate,
        OrderStatus status,
        List<OrderStatus> nextStatuses,
        BigDecimal totalAmount,
        String paymentMethod,
        PaymentStatus paymentStatus,
        String shippingAddress,
        List<OrderItemDTO> items) {

    public static AdminOrderView from(Order order) {
        return new AdminOrderView(
                order.getId(),
                order.getUser().getName(),
                order.getUser().getEmail(),
                order.getOrderDate(),
                order.getStatus(),
                Arrays.stream(OrderStatus.values())
                        .filter(order.getStatus()::canMoveTo)
                        // an online order isn't packed until it's paid
                        .filter(s -> !(s == OrderStatus.PROCESSING && order.getPaymentStatus() == PaymentStatus.AWAITING_PAYMENT))
                        .toList(),
                order.getTotalAmount(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getShippingAddress(),
                order.getOrderItems().stream().map(OrderItemDTO::new).toList());
    }
}
