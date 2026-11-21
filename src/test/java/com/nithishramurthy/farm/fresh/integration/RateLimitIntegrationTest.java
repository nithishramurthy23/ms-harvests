package com.nithishramurthy.farm.fresh.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nithishramurthy.farm.fresh.entity.User;
import com.nithishramurthy.farm.fresh.mainprog.FarmFreshApplication;
import com.nithishramurthy.farm.fresh.repository.OrderItemRepository;
import com.nithishramurthy.farm.fresh.repository.OrderRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The per-IP limit in front of the auth endpoints, and the "same answer for everyone" rule. */
@SpringBootTest(classes = FarmFreshApplication.class, properties = {
        "app.rate-limit.enabled=true",
        "app.rate-limit.send-per-10-min=3",
        "app.rate-limit.check-per-10-min=4"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitIntegrationTest {

    private static final String GENERIC = "If an account exists for this email, we've sent it a code. It's valid for 5 minutes.";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleChangeRepository roleChangeRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private StockMovementRepository stockMovementRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockBean private JavaMailSender mailSender;

    private static int ipCounter = 10;
    private String ip;

    @BeforeEach
    void setUp() {
        stockMovementRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        roleChangeRepository.deleteAll();
        userRepository.deleteAll();

        User asha = new User("asha@example.com", passwordEncoder.encode("Secret123!"), "Asha");
        asha.setEnabled(true);
        userRepository.save(asha);

        ip = "203.0.113." + (ipCounter++); // each test gets its own address, so counts don't carry over
    }

    @Test
    void tooManyCodeRequestsFromOneAddressGet429() throws Exception {
        for (int i = 0; i < 3; i++) {
            sendOtp("nobody" + i + "@example.com", ip).andExpect(status().isOk());
        }
        sendOtp("someone@example.com", ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Too many attempts")));

        // another address isn't affected
        sendOtp("someone@example.com", "198.51.100.1").andExpect(status().isOk());
    }

    @Test
    void passwordGuessingIsSlowedDown() throws Exception {
        for (int i = 0; i < 4; i++) {
            login("asha@example.com", "Wrong" + i + "!", ip).andExpect(status().isUnauthorized());
        }
        // even the right password has to wait now
        login("asha@example.com", "Secret123!", ip).andExpect(status().isTooManyRequests());
    }

    @Test
    void theReplyDoesNotSayWhetherAnAccountExists() throws Exception {
        String known = sendOtp("asha@example.com", ip).andReturn().getResponse().getContentAsString();
        String unknown = sendOtp("nobody@example.com", ip).andReturn().getResponse().getContentAsString();
        assertThat(known).isEqualTo(unknown).contains(GENERIC);

        login("nobody@example.com", "Secret123!", "198.51.100.2")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    void aSecondCodeWithinAMinuteIsQuietlySkipped() throws Exception {
        sendOtp("asha@example.com", ip).andExpect(status().isOk());
        String first = userRepository.findByEmail("asha@example.com").orElseThrow().getOtpCode();

        // same friendly answer, but no new code, so the first one still works
        sendOtp("asha@example.com", ip).andExpect(status().isOk()).andExpect(jsonPath("$.message").value(GENERIC));
        assertThat(userRepository.findByEmail("asha@example.com").orElseThrow().getOtpCode()).isEqualTo(first);
    }

    @Test
    void checkoutSaysHowLongToWaitForANewCode() throws Exception {
        String token = objectMapper.readTree(login("asha@example.com", "Secret123!", ip)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("token").asText();

        initiateOrder(token).andExpect(status().isOk());
        initiateOrder(token)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Please wait")));
    }

    @Test
    void theTestMailEndpointIsGone() throws Exception {
        String token = objectMapper.readTree(login("asha@example.com", "Secret123!", ip)
                .andReturn().getResponse().getContentAsString()).get("token").asText();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/test/mail")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private ResultActions sendOtp(String email, String fromIp) throws Exception {
        return mockMvc.perform(post("/api/auth/send-otp")
                .with(r -> { r.setRemoteAddr(fromIp); return r; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email))));
    }

    private ResultActions login(String email, String password, String fromIp) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .with(r -> { r.setRemoteAddr(fromIp); return r; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))));
    }

    private ResultActions initiateOrder(String token) throws Exception {
        return mockMvc.perform(post("/api/orders/initiate-order")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "asha@example.com"))));
    }
}
