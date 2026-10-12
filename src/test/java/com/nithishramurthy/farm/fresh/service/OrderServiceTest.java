package com.nithishramurthy.farm.fresh.service;

import com.nithishramurthy.farm.fresh.dto.OrderRequest;
import com.nithishramurthy.farm.fresh.dto.OrderResponse;
import com.nithishramurthy.farm.fresh.entity.Order;
import com.nithishramurthy.farm.fresh.entity.Product;
import com.nithishramurthy.farm.fresh.entity.User;
import com.nithishramurthy.farm.fresh.repository.OrderItemRepository;
import com.nithishramurthy.farm.fresh.repository.OrderRepository;
import com.nithishramurthy.farm.fresh.repository.ProductRepository;
import com.nithishramurthy.farm.fresh.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String EMAIL = "asha@example.com";

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private OtpService otpService;
    @Mock private MailService mailService;
    @Mock private ProductService productService;
    @Mock private PlatformTransactionManager transactionManager;

    @InjectMocks
    private OrderService orderService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(EMAIL, "hashed", "Asha");
        user.setId(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(EMAIL, null, List.of()));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void totalIsCalculatedFromServerPricesNotClientPrices() {
        Product plums = product(101L, "Ooty Plums", "199.00");
        Product bananas = product(102L, "Organic Bananas", "49.00");
        stubValidOtp();
        stubProduct(plums, "179");
        stubProduct(bananas, "49.00");
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> withId(inv.getArgument(0), 10L));

        // the client claims everything costs 1 rupee
        OrderRequest request = request("cod", item(101L, 2, "1"), item(102L, 3, "1"));

        OrderResponse response = orderService.placeOrder(request);

        // 2 x 179 + 3 x 49 = 505
        assertThat(response.getTotalAmount()).isEqualByComparingTo("505");
        assertThat(response.getOrderId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void discountedLinesAreMarked() {
        Product plums = product(101L, "Ooty Plums", "199.00");
        stubValidOtp();
        stubProduct(plums, "179");
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> withId(inv.getArgument(0), 10L));

        orderService.placeOrder(request("cod", item(101L, 1, null)));

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        assertThat(saved.getValue().getOrderItems()).singleElement()
                .satisfies(line -> {
                    assertThat(line.getProductPrice()).isEqualByComparingTo("179");
                    assertThat(line.isDiscountApplied()).isTrue();
                });
    }

    @Test
    void onlyCashOnDeliveryIsAccepted() {
        assertThatThrownBy(() -> orderService.placeOrder(request("card", item(101L, 1, null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cash on delivery");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void invalidOtpRejectsOrder() {
        when(otpService.validateOtp(EMAIL, "123456")).thenReturn(false);

        assertThatThrownBy(() -> orderService.placeOrder(request("cod", item(101L, 1, null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OTP");

        verify(productRepository, never()).decrementStock(anyLong(), anyInt());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void outOfStockRejectsOrder() {
        Product plums = product(101L, "Ooty Plums", "199.00");
        stubValidOtp();
        when(productRepository.findById(101L)).thenReturn(Optional.of(plums));
        when(productRepository.decrementStock(101L, 5)).thenReturn(0);

        assertThatThrownBy(() -> orderService.placeOrder(request("cod", item(101L, 5, null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not enough stock");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void zeroQuantityIsRejectedBeforeTouchingStock() {
        Product plums = product(101L, "Ooty Plums", "199.00");
        stubValidOtp();
        when(productRepository.findById(101L)).thenReturn(Optional.of(plums));

        assertThatThrownBy(() -> orderService.placeOrder(request("cod", item(101L, 0, null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity");

        verify(productRepository, never()).decrementStock(anyLong(), anyInt());
    }

    @Test
    void unknownProductRejectsOrder() {
        stubValidOtp();
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(request("cod", item(999L, 1, null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void mailFailureDoesNotFailTheOrder() {
        Product plums = product(101L, "Ooty Plums", "199.00");
        stubValidOtp();
        stubProduct(plums, "199.00");
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> withId(inv.getArgument(0), 10L));
        doThrow(new RuntimeException("SMTP down"))
                .when(mailService).sendOrderConfirmationEmail(any(), any(), any());

        OrderResponse response = orderService.placeOrder(request("cod", item(101L, 1, null)));

        assertThat(response.getOrderId()).isEqualTo(10L);
    }

    private void stubValidOtp() {
        when(otpService.validateOtp(EMAIL, "123456")).thenReturn(true);
    }

    private void stubProduct(Product product, String currentPrice) {
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.decrementStock(any(Long.class), anyInt())).thenReturn(1);
        when(productService.getCurrentPrice(product)).thenReturn(new BigDecimal(currentPrice));
    }

    private static Order withId(Order order, Long id) {
        order.setId(id);
        return order;
    }

    private static Product product(Long id, String name, String price) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setOriginalPrice(new BigDecimal(price));
        product.setStockQuantity(100);
        return product;
    }

    private static OrderRequest.OrderItemDto item(Long productId, int quantity, String clientPrice) {
        OrderRequest.OrderItemDto item = new OrderRequest.OrderItemDto();
        item.setProductId(productId);
        item.setQuantity(quantity);
        if (clientPrice != null) {
            item.setPrice(new BigDecimal(clientPrice));
        }
        return item;
    }

    private static OrderRequest request(String paymentMethod, OrderRequest.OrderItemDto... items) {
        OrderRequest.ShippingInfo shipping = new OrderRequest.ShippingInfo();
        shipping.setName("Asha");
        shipping.setAddress("12 MG Road");
        shipping.setCity("Bengaluru");
        shipping.setPostalCode("560001");
        shipping.setPhone("+91 98765 43210");
        shipping.setEmail(EMAIL);
        shipping.setExpectedDeliveryDate(LocalDate.now().plusDays(2));

        OrderRequest request = new OrderRequest();
        request.setShippingInfo(shipping);
        request.setItems(List.of(items));
        request.setPaymentMethod(paymentMethod);
        request.setOrderConfirmationOtp("123456");
        return request;
    }
}
