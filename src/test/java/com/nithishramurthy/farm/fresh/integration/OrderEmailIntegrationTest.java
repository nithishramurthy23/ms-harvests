package com.nithishramurthy.farm.fresh.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nithishramurthy.farm.fresh.entity.Category;
import com.nithishramurthy.farm.fresh.entity.Order;
import com.nithishramurthy.farm.fresh.entity.Role;
import com.nithishramurthy.farm.fresh.entity.User;
import com.nithishramurthy.farm.fresh.mainprog.FarmFreshApplication;
import com.nithishramurthy.farm.fresh.notification.OrderEmail;
import com.nithishramurthy.farm.fresh.payment.PaymentGateway;
import com.nithishramurthy.farm.fresh.repository.CategoryRepository;
import com.nithishramurthy.farm.fresh.repository.DiscountRuleRepository;
import com.nithishramurthy.farm.fresh.repository.OrderItemRepository;
import com.nithishramurthy.farm.fresh.repository.OrderRepository;
import com.nithishramurthy.farm.fresh.repository.ProductRepository;
import com.nithishramurthy.farm.fresh.repository.RoleChangeRepository;
import com.nithishramurthy.farm.fresh.repository.StockMovementRepository;
import com.nithishramurthy.farm.fresh.repository.UserRepository;
import com.nithishramurthy.farm.fresh.service.MailService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Which emails a customer gets, and when. Emails are captured at MailService, so
 * nothing is really sent. The point is the timing: an email goes out only after
 * the change it talks about is saved, and never for a change that failed.
 */
@SpringBootTest(classes = FarmFreshApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderEmailIntegrationTest {

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

    @MockBean private JavaMailSender mailSender;
    @MockBean private PaymentGateway gateway;
    @SpyBean private MailService mailService;

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

        admin = signUp("admin@example.com", Role.ADMIN);
        asha = signUp("asha@example.com", Role.USER);
        clearInvocations(mailService);
    }

    @Test
    void cashOnDeliveryGetsConfirmedShippedAndDelivered() throws Exception {
        long orderId = orderId(placeOrder(createProduct("Kiwi", 5), 2, "cod", null).andReturn());
        assertThat(subjects()).containsExactly("Farm Fresh: Order #" + orderId + " confirmed");

        for (String next : List.of("PROCESSING", "SHIPPED", "DELIVERED")) {
            send(admin, patch("/api/admin/orders/" + orderId + "/status"), Map.of("status", next))
                    .andExpect(status().isOk());
        }

        // packing doesn't need an email; shipping and delivery do
        assertThat(subjects()).containsExactly(
                "Farm Fresh: Order #" + orderId + " confirmed",
                "Farm Fresh: Order #" + orderId + " is on its way",
                "Farm Fresh: Order #" + orderId + " delivered");
        assertThat(emails().get(0).to()).isEqualTo("asha@example.com");
        assertThat(emails().get(0).text()).contains("2 x Kiwi").contains("ready to pay in cash");
    }

    @Test
    void onlineOrderIsConfirmedOnlyOnceItsPaid() throws Exception {
        long orderId = orderId(placeOrder(createProduct("Kiwi", 5), 1, "online", null).andReturn());
        assertThat(subjects()).isEmpty();

        reportPayment(orderId, "pay_1", GOOD_SIGNATURE).andExpect(status().isOk());
        reportPayment(orderId, "pay_1", GOOD_SIGNATURE).andExpect(status().isOk()); // reported twice

        assertThat(subjects()).containsExactly("Farm Fresh: Order #" + orderId + " confirmed");
        assertThat(emails().get(0).text()).contains("We've received your payment of ₹149.");
    }

    @Test
    void cancellingAPaidOrderSendsCancelledThenRefunded() throws Exception {
        long orderId = orderId(placeOrder(createProduct("Kiwi", 5), 1, "online", null).andReturn());
        reportPayment(orderId, "pay_1", GOOD_SIGNATURE).andExpect(status().isOk());
        clearInvocations(mailService);

        send(asha, post("/api/orders/" + orderId + "/cancel"), null).andExpect(status().isOk());

        assertThat(subjects()).containsExactly(
                "Farm Fresh: Order #" + orderId + " cancelled",
                "Farm Fresh: Refund for order #" + orderId + " sent");
        assertThat(emails().get(0).text()).contains("We're refunding ₹149");
    }

    @Test
    void noEmailForAnOrderThatFailed() throws Exception {
        long kiwi = createProduct("Kiwi", 1);
        placeOrder(kiwi, 5, "cod", null).andExpect(status().isBadRequest()); // not enough stock

        when(gateway.createOrder(anyLong(), anyString())).thenThrow(new RuntimeException("Razorpay down"));
        placeOrder(kiwi, 1, "online", null).andExpect(status().isBadRequest());

        assertThat(subjects()).isEmpty();
    }

    @Test
    void noEmailWhenAStatusChangeIsRefused() throws Exception {
        long orderId = orderId(placeOrder(createProduct("Kiwi", 5), 1, "cod", null).andReturn());
        clearInvocations(mailService);

        send(admin, patch("/api/admin/orders/" + orderId + "/status"), Map.of("status", "DELIVERED"))
                .andExpect(status().isConflict()); // can't skip packing and shipping

        assertThat(subjects()).isEmpty();
    }

    @Test
    void aMailProblemDoesNotStopTheOrder() throws Exception {
        doThrow(new RuntimeException("SMTP down")).when(mailService).sendOrderEmail(org.mockito.ArgumentMatchers.any());

        long orderId = orderId(placeOrder(createProduct("Kiwi", 5), 1, "cod", null)
                .andExpect(status().isCreated()).andReturn());
        send(admin, patch("/api/admin/orders/" + orderId + "/status"), Map.of("status", "PROCESSING"))
                .andExpect(status().isOk());

        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus().name()).isEqualTo("PROCESSING");
    }

    // --- helpers ---

    private List<OrderEmail> emails() {
        ArgumentCaptor<OrderEmail> captor = ArgumentCaptor.forClass(OrderEmail.class);
        try {
            verify(mailService, atLeast(0)).sendOrderEmail(captor.capture());
        } catch (AssertionError e) {
            return List.of();
        }
        return captor.getAllValues();
    }

    private List<String> subjects() {
        return emails().stream().map(OrderEmail::subject).toList();
    }

    private ResultActions reportPayment(long orderId, String paymentId, String signature) throws Exception {
        return send(asha, post("/api/payments/verify"), Map.of("orderId", orderId, "razorpayOrderId", "order_RZP1",
                "razorpayPaymentId", paymentId, "razorpaySignature", signature));
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
