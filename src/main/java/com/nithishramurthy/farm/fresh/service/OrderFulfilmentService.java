package com.nithishramurthy.farm.fresh.service;

import com.nithishramurthy.farm.fresh.dto.admin.AdminOrderView;
import com.nithishramurthy.farm.fresh.entity.Order;
import com.nithishramurthy.farm.fresh.entity.OrderItem;
import com.nithishramurthy.farm.fresh.entity.OrderStatus;
import com.nithishramurthy.farm.fresh.entity.PaymentStatus;
import com.nithishramurthy.farm.fresh.entity.Product;
import com.nithishramurthy.farm.fresh.entity.StockMovement;
import com.nithishramurthy.farm.fresh.exception.ResourceNotFoundException;
import com.nithishramurthy.farm.fresh.payment.OrderCancelledEvent;
import com.nithishramurthy.farm.fresh.repository.OrderRepository;
import com.nithishramurthy.farm.fresh.repository.ProductRepository;
import com.nithishramurthy.farm.fresh.repository.StockMovementRepository;

import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Moves orders through PENDING -> PROCESSING -> SHIPPED -> DELIVERED, or cancels them. */
@Service
public class OrderFulfilmentService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ApplicationEventPublisher events;
    private final EntityManager entityManager;

    public OrderFulfilmentService(OrderRepository orderRepository, ProductRepository productRepository,
                                  StockMovementRepository stockMovementRepository, ApplicationEventPublisher events,
                                  EntityManager entityManager) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.events = events;
        this.entityManager = entityManager;
    }

    /** All orders, newest first, optionally only one status. */
    @Transactional(readOnly = true)
    public List<AdminOrderView> listOrders(OrderStatus status) {
        List<Order> orders = status == null
                ? orderRepository.findAllByOrderByOrderDateDescIdDesc()
                : orderRepository.findByStatusOrderByOrderDateDescIdDesc(status);
        return orders.stream().map(AdminOrderView::from).toList();
    }

    /**
     * Admin moves an order on. Cancelling puts the items back in stock.
     *
     * @throws IllegalStateException if the move isn't allowed, e.g. SHIPPED -> PENDING
     */
    @Transactional
    public AdminOrderView changeStatus(Long orderId, OrderStatus next, String changedBy) {
        return AdminOrderView.from(moveTo(find(orderId), next, changedBy));
    }

    /** A customer cancels their own order. Only possible before the shop starts packing it. */
    @Transactional
    public void cancelByCustomer(Long orderId, Long userId, String email) {
        Order order = find(orderId);
        if (!order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found: " + orderId); // don't reveal other people's orders
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("This order is already being prepared and can't be cancelled online.");
        }
        moveTo(order, OrderStatus.CANCELLED, email);
    }

    /** Saves Razorpay's order id once the payment has been set up. */
    @Transactional
    public void attachGatewayOrder(Long orderId, String gatewayOrderId) {
        Order order = find(orderId);
        order.setGatewayOrderId(gatewayOrderId);
        orderRepository.save(order);
    }

    /**
     * Cancels an online order that was never paid and releases its stock. Does
     * nothing if the payment arrived in the meantime.
     */
    @Transactional
    public void cancelUnpaid(Long orderId, String reason) {
        if (orderRepository.movePayment(orderId, PaymentStatus.AWAITING_PAYMENT, PaymentStatus.UNPAID, null) == 0) {
            return;
        }
        Order order = find(orderId);
        if (order.getStatus().canMoveTo(OrderStatus.CANCELLED)) {
            moveTo(order, OrderStatus.CANCELLED, reason);
        }
    }

    private Order moveTo(Order order, OrderStatus next, String changedBy) {
        if (!order.getStatus().canMoveTo(next)) {
            throw new IllegalStateException("An order that is " + order.getStatus() + " can't become " + next + ".");
        }
        if (next == OrderStatus.PROCESSING && order.getPaymentStatus() == PaymentStatus.AWAITING_PAYMENT) {
            throw new IllegalStateException("This order hasn't been paid yet.");
        }
        if (next == OrderStatus.CANCELLED) {
            returnToStock(order, changedBy);
            // Conditional updates, so a payment landing at the same moment is never lost:
            // unpaid orders stay unpaid, paid ones are queued for a refund.
            if (orderRepository.movePayment(order.getId(), PaymentStatus.AWAITING_PAYMENT, PaymentStatus.UNPAID, null) == 0) {
                orderRepository.movePayment(order.getId(), PaymentStatus.PAID, PaymentStatus.REFUND_PENDING, null);
            }
            order = reload(order);
            events.publishEvent(new OrderCancelledEvent(order.getId()));
        }
        if (next == OrderStatus.DELIVERED) {
            // Cash on delivery is collected when the order arrives.
            orderRepository.movePayment(order.getId(), PaymentStatus.PAY_ON_DELIVERY, PaymentStatus.PAID, null);
            order = reload(order);
        }
        order.setStatus(next);
        return orderRepository.save(order);
    }

    /**
     * Picks up the payment status the conditional UPDATEs just wrote. The stock
     * UPDATE may have cleared the persistence context, so the old object can be detached.
     */
    private Order reload(Order order) {
        if (entityManager.contains(order)) {
            entityManager.refresh(order);
            return order;
        }
        return find(order.getId());
    }

    private void returnToStock(Order order, String changedBy) {
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            if (product == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                continue;
            }
            productRepository.adjustStock(product.getId(), item.getQuantity());
            Product updated = productRepository.findById(product.getId()).orElseThrow();
            stockMovementRepository.save(new StockMovement(updated, item.getQuantity(), updated.getStockQuantity(),
                    "Order #" + order.getId() + " cancelled", changedBy));
        }
    }

    private Order find(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }
}
