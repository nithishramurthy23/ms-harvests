package com.pooja.farm.fresh.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pooja.farm.fresh.entity.Product;
import com.pooja.farm.fresh.mainprog.FarmFreshApplication;
import com.pooja.farm.fresh.repository.OrderItemRepository;
import com.pooja.farm.fresh.repository.OrderRepository;
import com.pooja.farm.fresh.repository.ProductRepository;
import com.pooja.farm.fresh.repository.UserRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real app (controllers, security, JPA) against an in-memory database
 * and walks through what a customer does: sign up, verify, and place an order.
 * Only the outgoing mail server is faked.
 */
@SpringBootTest(classes = FarmFreshApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderFlowIntegrationTest {

    private static final String EMAIL = "asha@example.com";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;

    @MockBean
    private JavaMailSender mailSender;

    private Product plums;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        plums = new Product();
        plums.setName("Ooty Plums");
        plums.setPrice(new BigDecimal("199.00"));
        plums.setOriginalPrice(new BigDecimal("199.00"));
        plums.setWeight("1 kg");
        plums.setStockQuantity(10);
        plums = productRepository.save(plums);
    }

    @Test
    void customerCanSignUpAndPlaceAnOrder() throws Exception {
        String token = registerAndVerify();

        mockMvc.perform(post("/api/orders/initiate-order")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", EMAIL))))
                .andExpect(status().isOk());

        // the client sends a fake price of 1; the server must ignore it
        Map<String, Object> order = orderBody("cod", currentOtp(), 3, 1);

        mockMvc.perform(post("/api/orders/place-order")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(order)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(597.0));

        assertThat(productRepository.findById(plums.getId()).orElseThrow().getStockQuantity()).isEqualTo(7);

        mockMvc.perform(get("/api/orders/history").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void orderingMoreThanInStockFailsAndKeepsStock() throws Exception {
        String token = registerAndVerify();
        requestOrderOtp(token);

        mockMvc.perform(post("/api/orders/place-order")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(orderBody("cod", currentOtp(), 11, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Not enough stock for product: Ooty Plums"));

        assertThat(productRepository.findById(plums.getId()).orElseThrow().getStockQuantity()).isEqualTo(10);
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void ordersNeedALoggedInUser() throws Exception {
        mockMvc.perform(post("/api/orders/place-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(orderBody("cod", "123456", 1, null))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/orders/history").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cannotRequestOrderOtpForSomeoneElse() throws Exception {
        String token = registerAndVerify();

        mockMvc.perform(post("/api/orders/initiate-order")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "someone-else@example.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        registerAndVerify();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", EMAIL, "password", "WrongPass1!"))))
                .andExpect(status().isUnauthorized());
    }

    private String registerAndVerify() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Asha",
                                "email", EMAIL,
                                "password", "Secret123!",
                                "phone", "9876543210",
                                "address", "12 MG Road"))))
                .andExpect(status().isCreated());

        MvcResult verified = mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", EMAIL, "otp", currentOtp()))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(verified.getResponse().getContentAsString());
        return body.get("token").asText();
    }

    private void requestOrderOtp(String token) throws Exception {
        mockMvc.perform(post("/api/orders/initiate-order")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", EMAIL))))
                .andExpect(status().isOk());
    }

    // Stands in for the customer reading the OTP from their inbox.
    private String currentOtp() {
        return userRepository.findByEmail(EMAIL).orElseThrow().getOtpCode();
    }

    private Map<String, Object> orderBody(String paymentMethod, String otp, int quantity, Integer clientPrice) {
        Map<String, Object> item = new java.util.HashMap<>(Map.of("productId", plums.getId(), "quantity", quantity));
        if (clientPrice != null) {
            item.put("price", clientPrice);
        }
        return Map.of(
                "paymentMethod", paymentMethod,
                "orderConfirmationOtp", otp,
                "items", List.of(item),
                "shippingInfo", Map.of(
                        "name", "Asha",
                        "address", "12 MG Road",
                        "city", "Bengaluru",
                        "postalCode", "560001",
                        "phone", "+91 98765 43210",
                        "email", EMAIL,
                        "expectedDeliveryDate", LocalDate.now().plusDays(2).toString()));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
