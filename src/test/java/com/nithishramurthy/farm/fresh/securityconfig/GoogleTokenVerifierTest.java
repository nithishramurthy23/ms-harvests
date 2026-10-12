package com.nithishramurthy.farm.fresh.securityconfig;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Plays the part of Google: signs ID tokens with a test RSA key and checks the
 * verifier only accepts ones that are signed, fresh, from Google and for our app.
 */
class GoogleTokenVerifierTest {

    private static final String CLIENT_ID = "farm-fresh.apps.googleusercontent.com";

    private static KeyPair googleKeys;
    private static KeyPair attackerKeys;

    @BeforeAll
    static void makeKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        googleKeys = generator.generateKeyPair();
        attackerKeys = generator.generateKeyPair();
    }

    private final GoogleTokenVerifier verifier = new GoogleTokenVerifier(CLIENT_ID,
            NimbusJwtDecoder.withPublicKey((RSAPublicKey) googleKeys.getPublic()).build());

    @Test
    void acceptsAValidGoogleToken() throws Exception {
        GoogleTokenVerifier.GoogleUser user = verifier.verify(token(googleKeys, c -> {}));

        assertThat(user.email()).isEqualTo("asha@gmail.com");
        assertThat(user.name()).isEqualTo("Asha Rao");
    }

    @Test
    void rejectsTokenSignedBySomeoneElse() throws Exception {
        String forged = token(attackerKeys, c -> {});

        assertThatThrownBy(() -> verifier.verify(forged)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTokenMeantForAnotherApp() throws Exception {
        String otherApp = token(googleKeys, c -> c.audience("some-other-app.apps.googleusercontent.com"));

        assertThatThrownBy(() -> verifier.verify(otherApp)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTokenNotIssuedByGoogle() throws Exception {
        String wrongIssuer = token(googleKeys, c -> c.issuer("https://evil.example.com"));

        assertThatThrownBy(() -> verifier.verify(wrongIssuer)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        String expired = token(googleKeys, c -> c.expirationTime(Date.from(Instant.now().minusSeconds(3600))));

        assertThatThrownBy(() -> verifier.verify(expired)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnverifiedEmail() throws Exception {
        String unverified = token(googleKeys, c -> c.claim("email_verified", false));

        assertThatThrownBy(() -> verifier.verify(unverified))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not verified");
    }

    @Test
    void isDisabledWithoutAClientId() {
        GoogleTokenVerifier off = new GoogleTokenVerifier("", NimbusJwtDecoder.withPublicKey((RSAPublicKey) googleKeys.getPublic()).build());

        assertThat(off.isEnabled()).isFalse();
        assertThatThrownBy(() -> off.verify("anything")).isInstanceOf(IllegalStateException.class);
    }

    /** A Google-style ID token; each test tweaks one claim. */
    private static String token(KeyPair keys, Consumer<JWTClaimsSet.Builder> tweak) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer("https://accounts.google.com")
                .audience(CLIENT_ID)
                .subject("1234567890")
                .claim("email", "asha@gmail.com")
                .claim("email_verified", true)
                .claim("name", "Asha Rao")
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)));
        tweak.accept(claims);

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims.build());
        jwt.sign(new RSASSASigner(keys.getPrivate()));
        return jwt.serialize();
    }
}
