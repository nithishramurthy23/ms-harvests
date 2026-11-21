package com.nithishramurthy.farm.fresh.service;

import com.nithishramurthy.farm.fresh.dto.admin.AdminOrderView;
import com.nithishramurthy.farm.fresh.entity.Order;
import com.nithishramurthy.farm.fresh.entity.OrderItem;
import com.nithishramurthy.farm.fresh.entity.OrderStatus;
import com.nithishramurthy.farm.fresh.entity.Product;
import com.nithishramurthy.farm.fresh.entity.StockMovement;
import com.nithishramurthy.farm.fresh.exception.ResourceNotFoundException;
import com.nithishramurthy.farm.fresh.repository.OrderRepository;
import com.nithishramurthy.farm.fresh.repository.ProductRepository;
import com.nithishramurthy.farm.fresh.repository.StockMovementRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Moves orders through PENDING -> PROCESSING -> SHIPPED -> DELIVERED, or cancels them. */
@Service
public class OrderFulfilmentService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    public OrderFulfilmentService(OrderRepository orderRepository, ProductRepository productRepository,
                                  StockMovementRepository stockMovementRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
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
        Order order = find(orderId);
        moveTo(order, next, changedBy);
        return AdminOrderView.from(order);
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

    private void moveTo(Order order, OrderStatus next, String changedBy) {
        if (!order.getStatus().canMoveTo(next)) {
            throw new IllegalStateException("An order that is " + order.getStatus() + " can't become " + next + ".");
        }
        if (next == OrderStatus.CANCELLED) {
            returnToStock(order, changedBy);
        }
        order.setStatus(next);
        orderRepository.save(order);
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
