package com.nithishramurthy.farm.fresh.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nithishramurthy.farm.fresh.entity.Category;
import com.nithishramurthy.farm.fresh.entity.Order;
import com.nithishramurthy.farm.fresh.entity.PaymentStatus;
import com.nithishramurthy.farm.fresh.entity.Role;
import com.nithishramurthy.farm.fresh.entity.User;
import com.nithishramurthy.farm.fresh.mainprog.FarmFreshApplication;
import com.nithishramurthy.farm.fresh.payment.PaymentGateway;
import com.nithishramurthy.farm.fresh.payment.PaymentService;
import com.nithishramurthy.farm.fresh.repository.CategoryRepository;
import com.nithishramurthy.farm.fresh.repository.DiscountRuleRepository;
import com.nithishramurthy.farm.fresh.repository.OrderItemRepository;
import com.nithishramurthy.farm.fresh.repository.OrderRepository;
import com.nithishramurthy.farm.fresh.repository.ProductRepository;
import com.nithishramurthy.farm.fresh.repository.RoleChangeRepository;
import com.nithishramurthy.farm.fresh.repository.StockMovementRepository;
import com.nithishramurthy.farm.fresh.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Online payment from checkout to refund, with Razorpay replaced by a mock.
 * The interesting cases are the ones where things arrive twice, late, or not at all.
 */
@SpringBootTest(classes = FarmFreshApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentIntegrationTest {

    private static final String GOOD_SIGNATURE = "good-signature";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleChangeRepository roleChangeRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private DiscountRuleRepository discountRuleRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private StockMovementRepository stockMovementRepository;
    @Autowired private PaymentService paymentService;

    @MockBean private JavaMailSender mailSender;
    @MockBean private PaymentGateway gateway;

    private Long fruitsId;
    private String admin;
    private String asha;

    @BeforeEach
    void setUp() throws Exception {
        stockMovementRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        discountRuleRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        roleChangeRepository.deleteAll();
        userRepository.deleteAll();

        Category fruits = new Category();
        fruits.setName("Fruits");
        fruitsId = categoryRepository.save(fruits).getId();

        when(gateway.isEnabled()).thenReturn(true);
        when(gateway.publicKey()).thenReturn("rzp_test_key");
        when(gateway.createOrder(anyLong(), anyString())).thenReturn("order_RZP1");
        when(gateway.isValidPaymentSignature(anyString(), anyString(), eq(GOOD_SIGNATURE))).thenReturn(true);
        when(gateway.isValidWebhookSignature(anyString(), eq(GOOD_SIGNATURE))).thenReturn(true);

        admin = signUp("admin@example.com", Role.ADMIN);
        asha = signUp("asha@example.com", Role.USER);
    }

    @Test
    void checkoutConfigTellsTheBrowserOnlinePaymentIsOn() throws Exception {
        mockMvc.perform(get("/api/payments/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onlineEnabled").value(true))
                .andExpect(jsonPath("$.keyId").value("rzp_test_key"));
    }

    @Test
    void payOnlineThenShip() throws Exception {
        long kiwi = createProduct("Kiwi", 5);

        MvcResult placed = placeOrder(kiwi, 2, "online", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentStatus").value("AWAITING_PAYMENT"))
                .andExpect(jsonPath("$.payment.gatewayOrderId").value("order_RZP1"))
                .andExpect(jsonPath("$.payment.amountPaise").value(29800))
                .andReturn();
        long orderId = orderId(placed);
        verify(gateway).createOrder(29800, "order_" + orderId);
        assertThat(stockOf(kiwi)).isEqualTo(3); // reserved while the customer pays

        // can't be packed before it's paid
        send(admin, patch("/api/admin/orders/" + orderId + "/status"), Map.of("status", "PROCESSING"))
                .andExpect(status().isConflict());

        // a made-up signature is refused
        reportPayment(orderId, "pay_1", "forged").andExpect(status().isBadRequest());

        reportPayment(orderId, "pay_1", GOOD_SIGNATURE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));

        send(admin, patch("/api/admin/orders/" + orderId + "/status"), Map.of("status", "PROCESSING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
    }

    @Test
    void browserAndWebhookReportingTheSamePaymentCountsOnce() throws Exception {
        long orderId = orderId(placeOrder(createProduct("Kiwi", 5), 1, "online", null).andReturn());

        webhook("payment.captured", "order_RZP1", "pay_1", 14900).andExpect(status().isOk());
        reportPayment(orderId, "pay_1", GOOD_SIGNATURE).andExpect(jsonPath("$.paymentStatus").value("PAID"));
        webhook("payment.captured", "order_RZP1", "pay_1", 14900).andExpect(status().isOk());

        assertThat(order(orderId).getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order(orderId).getGatewayPaymentId()).isEqualTo("pay_1");
        verify(gateway, never()).refund(anyString(), anyLong());
    }

    @Test
    void webhookWithAWrongSignatureOrAmountIsIgnored() throws Exception {
        long orderId = orderId(placeOrder(createProduct("Kiwi", 5), 1, "online", null).andReturn());

        mockMvc.perform(post("/api/payments/webhook")
                        .header("X-Razorpay-Signature", "forged")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookBody("payment.captured", "order_RZP1", "pay_1", 14900)))
                .andExpect(status().isBadRequest());
        webhook("payment.captured", "order_RZP1", "pay_1", 100).andExpect(status().isOk());

        assertThat(order(orderId).getPaymentStatus()).isEqualTo(PaymentStatus.AWAITING_PAYMENT);
    }

    @Test
    void cancellingAPaidOrderRefundsIt() throws Exception {
        long kiwi = createProduct("Kiwi", 5);
        long orderId = orderId(placeOrder(kiwi, 2, "online", null).andReturn());
        reportPayment(orderId, "pay_1", GOOD_SIGNATURE).andExpect(status().isOk());

        send(asha, post("/api/orders/" + orderId + "/cancel"), null).andExpect(status().isOk());

        verify(gateway).refund("pay_1", 29800);
        assertThat(order(orderId).getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(stockOf(kiwi)).isEqualTo(5);
    }

    @Test
    void aFailedRefundCanBeRetriedByAnAdmin() throws Exception {
        long orderId = orderId(placeOrder(createProduct("Kiwi", 5), 1, "online", null).andReturn());
        reportPayment(orderId, "pay_1", GOOD_SIGNATURE).andExpect(status().isOk());
        doThrow(new RuntimeException("Razorpay is down")).when(gateway).refund(anyString(), anyLong());

        send(admin, patch("/api/admin/orders/" + orderId + "/status"), Map.of("status", "CANCELLED"))
                .andExpect(status().isOk());
        assertThat(order(orderId).getPaymentStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);

        org.mockito.Mockito.reset(gateway);
        send(admin, post("/api/admin/orders/" + orderId + "/refund"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("REFUNDED"));
        verify(gateway).refund("pay_1", 14900);

        // pressing it again does nothing
        send(admin, post("/api/admin/orders/" + orderId + "/refund"), null)
                .andExpect(jsonPath("$.paymentStatus").value("REFUNDED"));
        verify(gateway, times(1)).refund(anyString(), anyLong());
    }

    @Test
    void unpaidOrdersExpireAndReleaseTheirStock() throws Exception {
        long kiwi = createProduct("Kiwi", 5);
        long orderId = orderId(placeOrder(kiwi, 5, "online", null).andReturn());
        assertThat(stockOf(kiwi)).isZero();

        Order order = order(orderId);
        order.setOrderDate(LocalDateTime.now().minusHours(1));
        orderRepository.save(order);
        paymentService.expireUnpaidOrders();

        assertThat(order(orderId).getStatus().name()).isEqualTo("CANCELLED");
        assertThat(order(orderId).getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(stockOf(kiwi)).isEqualTo(5);

        // the customer pays a minute too late: they get their money back
        webhook("payment.captured", "order_RZP1", "pay_late", 74500).andExpect(status().isOk());
        verify(gateway).refund("pay_late", 74500);
        assertThat(order(orderId).getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void ifRazorpayIsDownTheOrderIsCancelledAndStockReleased() throws Exception {
        long kiwi = createProduct("Kiwi", 5);
        when(gateway.createOrder(anyLong(), anyString())).thenThrow(new RuntimeException("timeout"));

        placeOrder(kiwi, 2, "online", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cash on delivery")));
        assertThat(stockOf(kiwi)).isEqualTo(5);
    }

    @Test
    void retryingCheckoutWithTheSameKeyDoesNotCreateASecondOrder() throws Exception {
        long kiwi = createProduct("Kiwi", 5);

        long first = orderId(placeOrder(kiwi, 2, "cod", "checkout-abc").andExpect(status().isCreated()).andReturn());
        // the reply got lost and the browser sends the same request again (same key, old OTP)
        long again = orderId(placeOrder(kiwi, 2, "cod", "checkout-abc")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("This order was already placed."))
                .andReturn());

        assertThat(again).isEqualTo(first);
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(stockOf(kiwi)).isEqualTo(3);
    }

    @Test
    void cashOnDeliveryIsPaidWhenDelivered() throws Exception {
        long orderId = orderId(placeOrder(createProduct("Kiwi", 5), 1, "cod", null)
                .andExpect(jsonPath("$.paymentStatus").value("PAY_ON_DELIVERY"))
                .andReturn());
        for (String next : List.of("PROCESSING", "SHIPPED", "DELIVERED")) {
            send(admin, patch("/api/admin/orders/" + orderId + "/status"), Map.of("status", next))
                    .andExpect(status().isOk());
        }
        assertThat(order(orderId).getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void onlineIsRefusedWhenTheShopHasNoKeys() throws Exception {
        when(gateway.isEnabled()).thenReturn(false);
        placeOrder(createProduct("Kiwi", 5), 1, "online", null).andExpect(status().isBadRequest());
    }

    @Test
    void customersCantPayOrVerifySomeoneElsesOrder() throws Exception {
        long orderId = orderId(placeOrder(createProduct("Kiwi", 5), 1, "online", null).andReturn());
        String ravi = signUp("ravi@example.com", Role.USER);

        send(ravi, post("/api/payments/orders/" + orderId + "/retry"), null).andExpect(status().isNotFound());
        send(ravi, post("/api/payments/verify"), Map.of("orderId", orderId, "razorpayOrderId", "order_RZP1",
                "razorpayPaymentId", "pay_1", "razorpaySignature", GOOD_SIGNATURE))
                .andExpect(status().isNotFound());
        send(asha, post("/api/payments/orders/" + orderId + "/retry"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gatewayOrderId").value("order_RZP1"));
    }

    // --- helpers ---

    private ResultActions reportPayment(long orderId, String paymentId, String signature) throws Exception {
        return send(asha, post("/api/payments/verify"), Map.of("orderId", orderId, "razorpayOrderId", "order_RZP1",
                "razorpayPaymentId", paymentId, "razorpaySignature", signature));
    }

    private ResultActions webhook(String event, String gatewayOrderId, String paymentId, long amount) throws Exception {
        return mockMvc.perform(post("/api/payments/webhook")
                .header("X-Razorpay-Signature", GOOD_SIGNATURE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookBody(event, gatewayOrderId, paymentId, amount)));
    }

    private String webhookBody(String event, String gatewayOrderId, String paymentId, long amount) throws Exception {
        return json(Map.of("event", event, "payload", Map.of("payment", Map.of("entity",
                Map.of("id", paymentId, "order_id", gatewayOrderId, "amount", amount, "status", "captured")))));
    }

    private String signUp(String email, Role role) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Test", "email", email, "password", "Secret123!",
                                "phone", "9876543210", "address", "12 MG Road"))))
                .andExpect(status().isCreated());
        MvcResult verified = mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "otp", otpOf(email)))))
                .andExpect(status().isOk())
                .andReturn();
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setRole(role.authority());
        userRepository.save(user);
        return objectMapper.readTree(verified.getResponse().getContentAsString()).get("token").asText();
    }

    private long createProduct(String name, int stock) throws Exception {
        MvcResult created = send(admin, post("/api/admin/products"), Map.of(
                "name", name, "categoryId", fruitsId, "price", "149.00", "weight", "500 g", "initialStock", stock))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
    }

    private ResultActions placeOrder(long productId, int quantity, String method, String key) throws Exception {
        if (key == null || orderRepository.findAll().stream().noneMatch(o -> key.equals(o.getIdempotencyKey()))) {
            send(asha, post("/api/orders/initiate-order"), Map.of("email", "asha@example.com"))
                    .andExpect(status().isOk());
        }
        MockHttpServletRequestBuilder request = post("/api/orders/place-order");
        if (key != null) {
            request.header("Idempotency-Key", key);
        }
        return send(asha, request, Map.of(
                "paymentMethod", method,
                "orderConfirmationOtp", otpOf("asha@example.com") == null ? "000000" : otpOf("asha@example.com"),
                "items", List.of(Map.of("productId", productId, "quantity", quantity)),
                "shippingInfo", Map.of(
                        "name", "Asha", "address", "12 MG Road", "city", "Bengaluru", "postalCode", "560001",
                        "phone", "+91 98765 43210", "email", "asha@example.com",
                        "expectedDeliveryDate", LocalDate.now().plusDays(2).toString())));
    }

    private ResultActions send(String token, MockHttpServletRequestBuilder request, Object body) throws Exception {
        request.header("Authorization", "Bearer " + token);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json(body));
        }
        return mockMvc.perform(request);
    }

    private Order order(long id) {
        return orderRepository.findById(id).orElseThrow();
    }

    private int stockOf(long productId) {
        return productRepository.findById(productId).orElseThrow().getStockQuantity();
    }

    private String otpOf(String email) {
        return userRepository.findByEmail(email).orElseThrow().getOtpCode();
    }

    private long orderId(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("orderId").asLong();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
