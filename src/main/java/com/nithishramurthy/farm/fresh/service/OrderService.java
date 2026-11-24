package com.nithishramurthy.farm.fresh.service;

import com.nithishramurthy.farm.fresh.dto.OrderDTO;
import com.nithishramurthy.farm.fresh.dto.OrderRequest;
import com.nithishramurthy.farm.fresh.dto.OrderResponse;
import com.nithishramurthy.farm.fresh.entity.Order;
import com.nithishramurthy.farm.fresh.entity.OrderItem;
import com.nithishramurthy.farm.fresh.entity.OrderStatus;
import com.nithishramurthy.farm.fresh.entity.PaymentStatus;
import com.nithishramurthy.farm.fresh.entity.Product;
import com.nithishramurthy.farm.fresh.entity.User;
import com.nithishramurthy.farm.fresh.notification.OrderEmailEvent;
import com.nithishramurthy.farm.fresh.notification.OrderEmailType;
import com.nithishramurthy.farm.fresh.payment.PaymentService;
import com.nithishramurthy.farm.fresh.payment.PaymentStart;
import com.nithishramurthy.farm.fresh.repository.OrderItemRepository;
import com.nithishramurthy.farm.fresh.repository.OrderRepository;
import com.nithishramurthy.farm.fresh.repository.ProductRepository;
import com.nithishramurthy.farm.fresh.repository.UserRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Checkout and order lookup. */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final OtpService otpService;
    private final ApplicationEventPublisher events;
    private final ProductService productService;
    private final TransactionTemplate transactionTemplate;
    private final PaymentService paymentService;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository,
                        ProductRepository productRepository, OrderItemRepository orderItemRepository,
                        OtpService otpService, ApplicationEventPublisher events,
                        ProductService productService, PlatformTransactionManager transactionManager,
                        PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.otpService = otpService;
        this.events = events;
        this.productService = productService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.paymentService = paymentService;
    }

    /** Emails the order confirmation OTP. */
    @Transactional
    public void initiateOrderConfirmation(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        otpService.generateAndSendOtp(user.getEmail(), "Order Confirmation OTP", "order confirmation");
    }

    /**
     * Places an order for the logged-in user.
     *
     * The OTP check is deliberately outside the order transaction: a wrong guess
     * must stay counted (and the code burned) even though the order is rejected.
     * If it were inside, the rollback would undo the counter and the 6-digit code
     * could be guessed with unlimited tries.
     */
    public OrderResponse placeOrder(OrderRequest orderRequest) {
        return placeOrder(orderRequest, null);
    }

    /**
     * @param idempotencyKey a random id the browser makes once per checkout and sends
     *                       with every try. If the first request got through but its
     *                       reply was lost (slow network, double click), the retry
     *                       gets the same order back instead of creating a second one.
     */
    public OrderResponse placeOrder(OrderRequest orderRequest, String idempotencyKey) {
        // Step 1: who is ordering. The JWT filter already put the logged-in user in the
        // SecurityContext, so we take the email from there, never from the request body.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found."));

        // Step 2: is this a retry of a checkout that already went through?
        String key = normaliseKey(idempotencyKey);
        if (key != null) {
            // Checked before the OTP, because the first request already used the code up.
            Optional<Order> earlier = orderRepository.findByUserIdAndIdempotencyKey(user.getId(), key);
            if (earlier.isPresent()) {
                return alreadyPlaced(earlier.get());
            }
        }

        // Step 3: only two payment methods exist. Online needs Razorpay keys to be configured.
        boolean online = "online".equalsIgnoreCase(orderRequest.getPaymentMethod());
        if (online && !paymentService.isOnlineEnabled()) {
            throw new IllegalArgumentException("Online payment isn't set up for this shop. Please pick cash on delivery.");
        }
        if (!online && !"cod".equalsIgnoreCase(orderRequest.getPaymentMethod())) {
            throw new IllegalArgumentException("Pick cash on delivery or online payment.");
        }

        // Step 4: the emailed code. A stolen token alone can't place orders.
        if (orderRequest.getOrderConfirmationOtp() == null || orderRequest.getOrderConfirmationOtp().isEmpty()) {
            throw new IllegalArgumentException("Order confirmation OTP is required.");
        }

        // Commits on its own, whatever happens to the order below.
        boolean isOtpValid = otpService.validateOtp(user.getEmail(), orderRequest.getOrderConfirmationOtp());
        if (!isOtpValid) {
            throw new IllegalArgumentException("Invalid or expired OTP for order confirmation.");
        }

        // Step 5: take the stock and save the order, all in one transaction (createOrder below).
        Order savedOrder;
        try {
            savedOrder = transactionTemplate.execute(status -> createOrder(user, orderRequest, key, online));
        } catch (DataIntegrityViolationException e) {
            // Two tries with the same key arrived together and the other one won.
            if (key == null) {
                throw e;
            }
            return orderRepository.findByUserIdAndIdempotencyKey(user.getId(), key)
                    .map(this::alreadyPlaced)
                    .orElseThrow(() -> e);
        }

        // Step 6: the order is committed. Online orders now get a Razorpay payment;
        // cash-on-delivery orders get their confirmation email.
        if (online) {
            // Stock is reserved; now ask Razorpay for a payment. The confirmation
            // email goes out once the money arrives (PaymentService.markPaid).
            PaymentStart payment = paymentService.start(savedOrder);
            OrderResponse response = response(savedOrder, "Order saved. Complete the payment to confirm it.");
            response.setPayment(payment);
            return response;
        }

        events.publishEvent(new OrderEmailEvent(savedOrder.getId(), OrderEmailType.CONFIRMED));
        return response(savedOrder, "Order placed successfully!");
    }

    /**
     * Reply for a retried checkout: the order that was already created. If it's an online
     * order still waiting for money, the payment details are included again so the
     * customer can open the payment window and finish.
     */
    private OrderResponse alreadyPlaced(Order order) {
        OrderResponse response = response(order, "This order was already placed.");
        if (order.getPaymentStatus() == PaymentStatus.AWAITING_PAYMENT && order.getGatewayOrderId() != null) {
            response.setPayment(paymentService.resume(order.getId(), order.getUser().getId()));
        }
        return response;
    }

    /** The JSON the browser gets back after placing an order. */
    private static OrderResponse response(Order order, String message) {
        OrderResponse response = new OrderResponse(order.getId(), order.getStatus().name(), order.getTotalAmount(),
                order.getOrderDate(), message);
        response.setPaymentStatus(order.getPaymentStatus().name());
        return response;
    }

    /** Blank means "no key". Keys longer than the 64-character column are refused. */
    private static String normaliseKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        if (key.length() > 64) {
            throw new IllegalArgumentException("Idempotency-Key must be at most 64 characters.");
        }
        return key.trim();
    }

    /**
     * Reserves stock and saves the order with its items. Runs in one transaction:
     * if any item fails (unknown product, not enough stock), nothing is saved and
     * stock reserved for earlier items is released.
     */
    private Order createOrder(User user, OrderRequest orderRequest, String idempotencyKey, boolean online) {
        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setIdempotencyKey(idempotencyKey);
        order.setPaymentMethod(online ? "online" : "cod");
        order.setPaymentStatus(online ? PaymentStatus.AWAITING_PAYMENT : PaymentStatus.PAY_ON_DELIVERY);

        // The address is copied onto the order as text, so editing your profile later
        // doesn't change where an old order was sent.
        String fullShippingAddress = String.format("%s, %s, %s (Phone: %s, Email: %s)",
                orderRequest.getShippingInfo().getAddress(),
                orderRequest.getShippingInfo().getCity(),
                orderRequest.getShippingInfo().getPostalCode(),
                orderRequest.getShippingInfo().getPhone(),
                orderRequest.getShippingInfo().getEmail());
        order.setShippingAddress(fullShippingAddress);

        BigDecimal calculatedTotalAmount = BigDecimal.ZERO;

        // One pass per line in the basket. Any problem throws, and the throw rolls back
        // everything done so far in this transaction, including stock taken for earlier lines.
        for (OrderRequest.OrderItemDto itemDto : orderRequest.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemDto.getProductId()));

            if (!product.isActive()) {
                throw new IllegalArgumentException("Product is no longer sold: " + product.getName());
            }

            if (itemDto.getQuantity() == null || itemDto.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be at least 1 for product: " + product.getName());
            }

            // Reserve stock with a single conditional UPDATE. If the order fails later,
            // the transaction rolls this back.
            if (productRepository.decrementStock(product.getId(), itemDto.getQuantity()) == 0) {
                throw new IllegalArgumentException("Not enough stock for product: " + product.getName());
            }

            // Price comes from the database, never from the client request.
            BigDecimal unitPrice = productService.getCurrentPrice(product);

            // The line keeps its own copy of name, price, weight and picture, so the order
            // history still shows what was bought even if the product changes later.
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
        return savedOrder;
    }

    /** All orders of one customer, for the order history page. */
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    /** One order by id. The controller checks that it belongs to the caller before returning it. */
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
        return new OrderDTO(order);
    }
}
