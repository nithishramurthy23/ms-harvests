package com.nithishramurthy.farm.fresh.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nithishramurthy.farm.fresh.entity.Category;
import com.nithishramurthy.farm.fresh.entity.Role;
import com.nithishramurthy.farm.fresh.entity.User;
import com.nithishramurthy.farm.fresh.mainprog.FarmFreshApplication;
import com.nithishramurthy.farm.fresh.repository.CategoryRepository;
import com.nithishramurthy.farm.fresh.repository.DiscountRuleRepository;
import com.nithishramurthy.farm.fresh.repository.OrderItemRepository;
import com.nithishramurthy.farm.fresh.repository.OrderRepository;
import com.nithishramurthy.farm.fresh.repository.ProductRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole store cycle through the real API: an admin adds a product and
 * stock, a customer buys it until it's sold out, the admin ships the order,
 * and cancelling puts stock back. Also checks who is allowed to do what.
 */
@SpringBootTest(classes = FarmFreshApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StoreAdminIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private DiscountRuleRepository discountRuleRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private StockMovementRepository stockMovementRepository;

    @MockBean
    private JavaMailSender mailSender;

    private Long fruitsId;

    @BeforeEach
    void setUp() {
        stockMovementRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        discountRuleRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        Category fruits = new Category();
        fruits.setName("Fruits");
        fruitsId = categoryRepository.save(fruits).getId();
    }

    @Test
    void fullCycleFromStockToDelivery() throws Exception {
        String admin = signUp("admin@example.com", Role.ADMIN);
        String customer = signUp("asha@example.com", Role.USER);

        // Admin adds a product with 2 in stock
        long kiwiId = id(send(admin, post("/api/admin/products"), productBody("Kiwi", "149.00", 2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stock").value(2))
                .andReturn());

        mockMvc.perform(get("/api/products/" + kiwiId))
                .andExpect(jsonPath("$.inStock").value(true));

        // Customer buys both, so it's sold out
        long orderId = placeOrder(customer, "asha@example.com", kiwiId, 2)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString().transform(this::orderIdFrom);

        mockMvc.perform(get("/api/products/" + kiwiId))
                .andExpect(jsonPath("$.stock").value(0))
                .andExpect(jsonPath("$.inStock").value(false));

        // Admin restocks
        send(admin, post("/api/admin/products/" + kiwiId + "/stock"), Map.of("change", 10, "reason", "Tuesday harvest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(10));

        send(admin, get("/api/admin/products/" + kiwiId + "/stock"), null)
                .andExpect(jsonPath("$[0].change").value(10))
                .andExpect(jsonPath("$[0].reason").value("Tuesday harvest"))
                .andExpect(jsonPath("$[0].changedBy").value("admin@example.com"))
                .andExpect(jsonPath("$[1].reason").value("Initial stock"));

        // Admin moves the order through to delivered
        send(admin, get("/api/admin/orders?status=PENDING"), null)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customerEmail").value("asha@example.com"))
                .andExpect(jsonPath("$[0].nextStatuses[0]").value("PROCESSING"));

        for (String next : List.of("PROCESSING", "SHIPPED", "DELIVERED")) {
            send(admin, patch("/api/admin/orders/" + orderId + "/status"), Map.of("status", next))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(next));
        }

        // A delivered order can't go back
        send(admin, patch("/api/admin/orders/" + orderId + "/status"), Map.of("status", "CANCELLED"))
                .andExpect(status().isConflict());
    }

    @Test
    void cancellingAnOrderPutsTheStockBack() throws Exception {
        String admin = signUp("admin@example.com", Role.ADMIN);
        String customer = signUp("asha@example.com", Role.USER);
        long kiwiId = createProduct(admin, "Kiwi", 5);

        long first = orderIdFrom(placeOrder(customer, "asha@example.com", kiwiId, 3).andReturn().getResponse().getContentAsString());
        assertThat(stockOf(kiwiId)).isEqualTo(2);

        // the customer cancels their own pending order
        send(customer, post("/api/orders/" + first + "/cancel"), null).andExpect(status().isOk());
        assertThat(stockOf(kiwiId)).isEqualTo(5);

        // an admin cancels one that's already being packed
        long second = orderIdFrom(placeOrder(customer, "asha@example.com", kiwiId, 4).andReturn().getResponse().getContentAsString());
        send(admin, patch("/api/admin/orders/" + second + "/status"), Map.of("status", "PROCESSING")).andExpect(status().isOk());
        send(customer, post("/api/orders/" + second + "/cancel"), null).andExpect(status().isConflict());
        send(admin, patch("/api/admin/orders/" + second + "/status"), Map.of("status", "CANCELLED")).andExpect(status().isOk());

        assertThat(stockOf(kiwiId)).isEqualTo(5);
    }

    @Test
    void customersCantCancelSomeoneElsesOrder() throws Exception {
        String admin = signUp("admin@example.com", Role.ADMIN);
        String asha = signUp("asha@example.com", Role.USER);
        String ravi = signUp("ravi@example.com", Role.USER);
        long kiwiId = createProduct(admin, "Kiwi", 5);
        long order = orderIdFrom(placeOrder(asha, "asha@example.com", kiwiId, 1).andReturn().getResponse().getContentAsString());

        send(ravi, post("/api/orders/" + order + "/cancel"), null).andExpect(status().isNotFound());
        assertThat(stockOf(kiwiId)).isEqualTo(4);
    }

    @Test
    void stockCantGoBelowZero() throws Exception {
        String admin = signUp("admin@example.com", Role.ADMIN);
        long kiwiId = createProduct(admin, "Kiwi", 3);

        send(admin, post("/api/admin/products/" + kiwiId + "/stock"), Map.of("change", -5))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only 3 in stock, can't remove 5."));
        assertThat(stockOf(kiwiId)).isEqualTo(3);
    }

    @Test
    void hiddenProductsLeaveTheShopAndCantBeBought() throws Exception {
        String admin = signUp("admin@example.com", Role.ADMIN);
        String customer = signUp("asha@example.com", Role.USER);
        long kiwiId = createProduct(admin, "Kiwi", 5);

        send(admin, patch("/api/admin/products/" + kiwiId + "/active"), Map.of("active", false))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/products")).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/products/" + kiwiId)).andExpect(status().isNotFound());
        placeOrder(customer, "asha@example.com", kiwiId, 1)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Product is no longer sold: Kiwi"));

        // still listed for admins
        send(admin, get("/api/admin/products"), null).andExpect(jsonPath("$[0].active").value(false));
    }

    @Test
    void adminCanEditDetailsAndPrice() throws Exception {
        String admin = signUp("admin@example.com", Role.ADMIN);
        long kiwiId = createProduct(admin, "Kiwi", 5);

        send(admin, put("/api/admin/products/" + kiwiId), productBody("Golden Kiwi", "179.00", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Golden Kiwi"))
                .andExpect(jsonPath("$.stock").value(5));

        mockMvc.perform(get("/api/products/" + kiwiId))
                .andExpect(jsonPath("$.price").value(179.0));
    }

    @Test
    void invalidProductIsRejectedWithFieldErrors() throws Exception {
        String admin = signUp("admin@example.com", Role.ADMIN);
        Map<String, Object> body = new HashMap<>(productBody("", "0", 1));

        send(admin, post("/api/admin/products"), body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").exists())
                .andExpect(jsonPath("$.fields.price").exists());
    }

    @Test
    void usersAndMembersCantManageTheStore() throws Exception {
        String admin = signUp("admin@example.com", Role.ADMIN);
        long kiwiId = createProduct(admin, "Kiwi", 5);

        for (Role role : List.of(Role.USER, Role.MEMBER)) {
            String token = signUp(role.name().toLowerCase() + "@example.com", role);

            send(token, get("/api/admin/products"), null).andExpect(status().isForbidden());
            send(token, post("/api/admin/products/" + kiwiId + "/stock"), Map.of("change", 100))
                    .andExpect(status().isForbidden());
            send(token, get("/api/admin/orders"), null).andExpect(status().isForbidden());
            send(token, get("/api/owner/users"), null).andExpect(status().isForbidden());
        }
        assertThat(stockOf(kiwiId)).isEqualTo(5);

        mockMvc.perform(get("/api/admin/products")).andExpect(status().isUnauthorized());
    }

    @Test
    void onlyTheOwnerManagesRoles() throws Exception {
        String owner = signUp("owner@example.com", Role.OWNER);
        String admin = signUp("admin@example.com", Role.ADMIN);
        signUp("asha@example.com", Role.USER);
        Long ashaId = userRepository.findByEmail("asha@example.com").orElseThrow().getId();
        Long ownerId = userRepository.findByEmail("owner@example.com").orElseThrow().getId();

        // admins run the store but can't hand out roles
        send(admin, patch("/api/owner/users/" + ashaId + "/role"), Map.of("role", "ADMIN"))
                .andExpect(status().isForbidden());

        send(owner, patch("/api/owner/users/" + ashaId + "/role"), Map.of("role", "MEMBER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEMBER"));
        assertThat(userRepository.findById(ashaId).orElseThrow().getRole()).isEqualTo("ROLE_MEMBER");

        // the owner can't lock themselves out
        send(owner, patch("/api/owner/users/" + ownerId + "/role"), Map.of("role", "USER"))
                .andExpect(status().isConflict());

        // owners can also do everything admins do
        send(owner, get("/api/admin/products"), null).andExpect(status().isOk());
        send(owner, get("/api/owner/summary"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordersByStatus.PENDING").value(0))
                .andExpect(jsonPath("$.customers").value(1));
    }

    @Test
    void loginTellsTheFrontendTheRole() throws Exception {
        signUp("admin@example.com", Role.ADMIN);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "admin@example.com", "password", "Secret123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    // --- helpers ---

    /** Registers and verifies an account, gives it a role, and returns its JWT. */
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

    private long createProduct(String adminToken, String name, int stock) throws Exception {
        return id(send(adminToken, post("/api/admin/products"), productBody(name, "149.00", stock))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private ResultActions placeOrder(String token, String email, long productId, int quantity) throws Exception {
        send(token, post("/api/orders/initiate-order"), Map.of("email", email)).andExpect(status().isOk());

        return send(token, post("/api/orders/place-order"), Map.of(
                "paymentMethod", "cod",
                "orderConfirmationOtp", otpOf(email),
                "items", List.of(Map.of("productId", productId, "quantity", quantity)),
                "shippingInfo", Map.of(
                        "name", "Asha", "address", "12 MG Road", "city", "Bengaluru", "postalCode", "560001",
                        "phone", "+91 98765 43210", "email", email,
                        "expectedDeliveryDate", LocalDate.now().plusDays(2).toString())));
    }

    private ResultActions send(String token, MockHttpServletRequestBuilder request, Object body) throws Exception {
        request.header("Authorization", "Bearer " + token);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json(body));
        }
        return mockMvc.perform(request);
    }

    private Map<String, Object> productBody(String name, String price, Integer stock) {
        Map<String, Object> body = new HashMap<>(Map.of(
                "name", name, "weight", "500 g", "categoryId", fruitsId, "price", price));
        if (stock != null) {
            body.put("initialStock", stock);
        }
        return body;
    }

    private int stockOf(long productId) {
        return productRepository.findById(productId).orElseThrow().getStockQuantity();
    }

    private String otpOf(String email) {
        return userRepository.findByEmail(email).orElseThrow().getOtpCode();
    }

    private long id(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long orderIdFrom(String responseBody) {
        try {
            JsonNode node = objectMapper.readTree(responseBody);
            return node.get("orderId").asLong();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
