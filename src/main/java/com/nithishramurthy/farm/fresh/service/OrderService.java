// src/main/java/com/nithishramurthy/farm/fresh/service/OrderService.java

package com.nithishramurthy.farm.fresh.service;

import com.nithishramurthy.farm.fresh.dto.OrderDTO;
import com.nithishramurthy.farm.fresh.dto.OrderRequest;
import com.nithishramurthy.farm.fresh.dto.OrderResponse;
import com.nithishramurthy.farm.fresh.entity.Order;
import com.nithishramurthy.farm.fresh.entity.OrderItem;
import com.nithishramurthy.farm.fresh.entity.OrderStatus;
import com.nithishramurthy.farm.fresh.entity.Product;
import com.nithishramurthy.farm.fresh.entity.User;
import com.nithishramurthy.farm.fresh.repository.OrderItemRepository;
import com.nithishramurthy.farm.fresh.repository.OrderRepository;
import com.nithishramurthy.farm.fresh.repository.ProductRepository;
import com.nithishramurthy.farm.fresh.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final OtpService otpService;
    private final MailService mailService;
    private final ProductService productService;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository,
                        ProductRepository productRepository, OrderItemRepository orderItemRepository,
                        OtpService otpService, MailService mailService,
                        ProductService productService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.otpService = otpService;
        this.mailService = mailService;
        this.productService = productService;
    }

    @Transactional
    public void initiateOrderConfirmation(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        otpService.generateAndSendOtp(user.getEmail(), "Order Confirmation OTP", "order confirmation");
    }

    @Transactional
    public OrderResponse placeOrder(OrderRequest orderRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found."));

        // Card/UPI need a real payment gateway; until then only cash on delivery is allowed.
        if (!"cod".equalsIgnoreCase(orderRequest.getPaymentMethod())) {
            throw new IllegalArgumentException("Only cash on delivery is available right now.");
        }

        if (orderRequest.getOrderConfirmationOtp() == null || orderRequest.getOrderConfirmationOtp().isEmpty()) {
            throw new IllegalArgumentException("Order confirmation OTP is required.");
        }

        boolean isOtpValid = otpService.validateOtp(user.getEmail(), orderRequest.getOrderConfirmationOtp());
        if (!isOtpValid) {
            throw new IllegalArgumentException("Invalid or expired OTP for order confirmation.");
        }

        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setPaymentMethod(orderRequest.getPaymentMethod());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.ZERO);

        String fullShippingAddress = String.format("%s, %s, %s (Phone: %s, Email: %s)",
                orderRequest.getShippingInfo().getAddress(),
                orderRequest.getShippingInfo().getCity(),
                orderRequest.getShippingInfo().getPostalCode(),
                orderRequest.getShippingInfo().getPhone(),
                orderRequest.getShippingInfo().getEmail());
        order.setShippingAddress(fullShippingAddress);

        BigDecimal calculatedTotalAmount = BigDecimal.ZERO;

        for (OrderRequest.OrderItemDto itemDto : orderRequest.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemDto.getProductId()));

            if (itemDto.getQuantity() == null || itemDto.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be at least 1 for product: " + product.getName());
            }

            // Reserve stock with a single conditional UPDATE. If the order fails later,
            // the surrounding transaction rolls this back.
            if (productRepository.decrementStock(product.getId(), itemDto.getQuantity()) == 0) {
                throw new IllegalArgumentException("Not enough stock for product: " + product.getName());
            }

            // Price comes from the database, never from the client request.
            BigDecimal unitPrice = productService.getCurrentPrice(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setProductPrice(unitPrice);
            orderItem.setQuantity(itemDto.getQuantity());
            orderItem.setProductWeight(product.getWeight());
            orderItem.setProductImageUrl(product.getImageUrl());
            orderItem.setDiscountApplied(unitPrice.compareTo(product.getOriginalPrice()) != 0);

            calculatedTotalAmount = calculatedTotalAmount.add(
                    unitPrice.multiply(BigDecimal.valueOf(itemDto.getQuantity()))
            );

            order.getOrderItems().add(orderItem);
        }

        order.setTotalAmount(calculatedTotalAmount);

        Order savedOrder = orderRepository.save(order);
        orderItemRepository.saveAll(savedOrder.getOrderItems());

        try {
            mailService.sendOrderConfirmationEmail(savedOrder, user.getEmail(), user.getName());
        } catch (Exception e) {
            System.err.println("Failed to send order confirmation email for order " + savedOrder.getId() + ": " + e.getMessage());
        }

        return new OrderResponse(
                savedOrder.getId(),
                savedOrder.getStatus().name(),
                savedOrder.getTotalAmount(),
                savedOrder.getOrderDate(),
                "Order placed successfully!"
        );
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
        return new OrderDTO(order);
    }
}
