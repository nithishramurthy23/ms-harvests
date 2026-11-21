package com.nithishramurthy.farm.fresh.dto.admin;

import com.nithishramurthy.farm.fresh.dto.OrderItemDTO;
import com.nithishramurthy.farm.fresh.entity.Order;
import com.nithishramurthy.farm.fresh.entity.OrderStatus;

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
        String shippingAddress,
        List<OrderItemDTO> items) {

    public static AdminOrderView from(Order order) {
        return new AdminOrderView(
                order.getId(),
                order.getUser().getName(),
                order.getUser().getEmail(),
                order.getOrderDate(),
                order.getStatus(),
                Arrays.stream(OrderStatus.values()).filter(order.getStatus()::canMoveTo).toList(),
                order.getTotalAmount(),
                order.getPaymentMethod(),
                order.getShippingAddress(),
                order.getOrderItems().stream().map(OrderItemDTO::new).toList());
    }
}
