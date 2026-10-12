package com.pooja.farm.fresh.service;

import com.pooja.farm.fresh.entity.User;
import com.pooja.farm.fresh.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    private static final String EMAIL = "asha@example.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private MailService mailService;

    @InjectMocks
    private OtpService otpService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(EMAIL, "hashed", "Asha");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    @Test
    void generatedOtpIsSixDigitsAndExpiresInFiveMinutes() {
        otpService.generateAndSendOtp(EMAIL, "Subject", "login");

        assertThat(user.getOtpCode()).matches("\\d{6}");
        assertThat(user.getOtpExpiry())
                .isCloseTo(Instant.now().plus(Duration.ofMinutes(5)), within(5, ChronoUnit.SECONDS));
    }

    @Test
    void generatedOtpIsEmailedToTheUser() {
        otpService.generateAndSendOtp(EMAIL, "Login OTP", "login");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendTestMail(eq(EMAIL), eq("Login OTP"), body.capture());
        assertThat(body.getValue()).contains(user.getOtpCode());
    }

    @Test
    void correctOtpIsAcceptedOnceAndThenCleared() {
        user.setOtpCode("482913");
        user.setOtpExpiry(Instant.now().plusSeconds(60));

        assertThat(otpService.validateOtp(EMAIL, "482913")).isTrue();
        assertThat(user.getOtpCode()).isNull();

        // the same code can't be replayed
        assertThat(otpService.validateOtp(EMAIL, "482913")).isFalse();
    }

    @Test
    void wrongOtpIsRejectedAndCountsAsFailedAttempt() {
        user.setOtpCode("482913");
        user.setOtpExpiry(Instant.now().plusSeconds(60));

        assertThat(otpService.validateOtp(EMAIL, "000000")).isFalse();
        assertThat(user.getFailedAttempts()).isEqualTo(1);
        assertThat(user.getOtpCode()).isNull();
    }

    @Test
    void expiredOtpIsRejected() {
        user.setOtpCode("482913");
        user.setOtpExpiry(Instant.now().minusSeconds(1));

        assertThat(otpService.validateOtp(EMAIL, "482913")).isFalse();
    }

    @Test
    void tooManyFailedAttemptsLocksOtpCheck() {
        user.setOtpCode("482913");
        user.setOtpExpiry(Instant.now().plusSeconds(60));
        user.setFailedAttempts(2);

        assertThatThrownBy(() -> otpService.validateOtp(EMAIL, "482913"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Maximum OTP attempts");
    }

    @Test
    void successfulOtpResetsFailedAttempts() {
        user.setOtpCode("482913");
        user.setOtpExpiry(Instant.now().plusSeconds(60));
        user.setFailedAttempts(1);

        assertThat(otpService.validateOtp(EMAIL, "482913")).isTrue();
        assertThat(user.getFailedAttempts()).isZero();
    }
}
