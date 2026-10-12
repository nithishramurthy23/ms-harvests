package com.pooja.farm.fresh.securityconfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = newProvider(randomSecret(64), 60_000);
    }

    @Test
    void tokenRoundTripsTheEmail() {
        String token = tokenProvider.generateToken("asha@example.com");

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUsernameFromJWT(token)).isEqualTo("asha@example.com");
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = tokenProvider.generateToken("asha@example.com");
        String[] parts = token.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"admin@example.com\"}".getBytes());

        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThat(tokenProvider.validateToken(forged)).isFalse();
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtTokenProvider otherServer = newProvider(randomSecret(64), 60_000);
        String token = otherServer.generateToken("asha@example.com");

        assertThat(tokenProvider.validateToken(token)).isFalse();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtTokenProvider shortLived = newProvider(randomSecret(64), -1_000);
        String token = shortLived.generateToken("asha@example.com");

        assertThat(shortLived.validateToken(token)).isFalse();
    }

    @Test
    void garbageIsRejected() {
        assertThat(tokenProvider.validateToken("not-a-jwt")).isFalse();
        assertThat(tokenProvider.validateToken("")).isFalse();
    }

    @Test
    void weakSecretIsRefusedAtStartup() {
        assertThatThrownBy(() -> newProvider(randomSecret(16), 60_000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("512 bits");
    }

    private static JwtTokenProvider newProvider(String secret, long expirationMs) {
        return new JwtTokenProvider(secret, expirationMs);
    }

    private static String randomSecret(int bytes) {
        byte[] key = new byte[bytes];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
