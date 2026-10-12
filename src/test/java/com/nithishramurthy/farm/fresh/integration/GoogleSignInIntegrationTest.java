package com.nithishramurthy.farm.fresh.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nithishramurthy.farm.fresh.entity.User;
import com.nithishramurthy.farm.fresh.mainprog.FarmFreshApplication;
import com.nithishramurthy.farm.fresh.repository.OrderItemRepository;
import com.nithishramurthy.farm.fresh.repository.OrderRepository;
import com.nithishramurthy.farm.fresh.repository.UserRepository;
import com.nithishramurthy.farm.fresh.securityconfig.GoogleTokenVerifier;
import com.nithishramurthy.farm.fresh.securityconfig.GoogleTokenVerifier.GoogleUser;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sign in with Google through the real controllers, security and database.
 * Google itself is faked: the verifier is mocked (GoogleTokenVerifierTest covers it).
 */
@SpringBootTest(classes = FarmFreshApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GoogleSignInIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;

    @MockBean private GoogleTokenVerifier googleTokenVerifier;
    @MockBean private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void firstGoogleLoginCreatesAnAccountAndReturnsAWorkingToken() throws Exception {
        when(googleTokenVerifier.verify("good-token")).thenReturn(new GoogleUser("asha@gmail.com", "Asha Rao"));

        String token = googleLogin("good-token");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("asha@gmail.com"))
                .andExpect(jsonPath("$.name").value("Asha Rao"));

        User saved = userRepository.findByEmail("asha@gmail.com").orElseThrow();
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    void googleLoginReusesAnExistingEmailAccount() throws Exception {
        User existing = new User("asha@gmail.com", "hashed", "Asha");
        existing.setPhone("9876543210");
        existing = userRepository.save(existing); // signed up with a password but never entered the OTP

        when(googleTokenVerifier.verify("good-token")).thenReturn(new GoogleUser("asha@gmail.com", "Asha Rao"));
        googleLogin("good-token");

        assertThat(userRepository.count()).isEqualTo(1);
        User after = userRepository.findById(existing.getId()).orElseThrow();
        assertThat(after.isEnabled()).isTrue();
        assertThat(after.getPhone()).isEqualTo("9876543210");
        assertThat(after.getPassword()).isEqualTo("hashed");
    }

    @Test
    void badGoogleTokenIsRejected() throws Exception {
        when(googleTokenVerifier.verify("forged")).thenThrow(new IllegalArgumentException("Invalid Google sign-in. Please try again."));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("credential", "forged"))))
                .andExpect(status().isUnauthorized());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void configEndpointTellsTheFrontendWhetherToShowTheButton() throws Exception {
        when(googleTokenVerifier.isEnabled()).thenReturn(true);
        when(googleTokenVerifier.getClientId()).thenReturn("abc.apps.googleusercontent.com");

        mockMvc.perform(get("/api/auth/google/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.clientId").value("abc.apps.googleusercontent.com"));
    }

    private String googleLogin(String credential) throws Exception {
        String body = mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("credential", credential))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }
}
