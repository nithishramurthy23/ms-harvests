package com.nithishramurthy.farm.fresh.securityconfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * Checks the ID token the "Sign in with Google" button hands the browser.
 * The token is a JWT signed by Google: we check the signature against Google's
 * public keys (fetched once and cached), that it was issued by Google, that it
 * was issued for OUR client ID, and that it hasn't expired. Only then do we
 * trust the email inside it.
 */
@Component
public class GoogleTokenVerifier {

    static final String GOOGLE_KEYS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    /** What we use from a verified Google token. */
    public record GoogleUser(String email, String name) {}

    private final String clientId;
    private final JwtDecoder decoder;

    @Autowired
    public GoogleTokenVerifier(@Value("${app.google.client-id:}") String clientId) {
        this(clientId, NimbusJwtDecoder.withJwkSetUri(GOOGLE_KEYS_URL).build());
    }

    // Tests pass a decoder that trusts their own signing key instead of Google's.
    GoogleTokenVerifier(String clientId, JwtDecoder decoder) {
        this.clientId = clientId == null ? "" : clientId.trim();
        if (decoder instanceof NimbusJwtDecoder nimbus) {
            nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    JwtValidators.createDefault(), // expiry / not-before
                    claim(jwt -> GOOGLE_ISSUERS.contains(String.valueOf(jwt.getClaims().get("iss"))), "Token not issued by Google"),
                    claim(jwt -> jwt.getAudience() != null && jwt.getAudience().contains(this.clientId), "Token issued for a different app")
            ));
        }
        this.decoder = decoder;
    }

    /** True when GOOGLE_CLIENT_ID is set, so the frontend should show the button. */
    public boolean isEnabled() {
        return StringUtils.hasText(clientId);
    }

    public String getClientId() {
        return clientId;
    }

    /** Returns the user's email and name, or throws IllegalArgumentException if the token can't be trusted. */
    public GoogleUser verify(String idToken) {
        if (!isEnabled()) {
            throw new IllegalStateException("Google sign-in is not configured (GOOGLE_CLIENT_ID is empty).");
        }
        if (!StringUtils.hasText(idToken)) {
            throw new IllegalArgumentException("Missing Google credential.");
        }

        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid Google sign-in. Please try again.");
        }

        // Google only vouches for the email if it says it checked it.
        String email = jwt.getClaimAsString("email");
        if (!StringUtils.hasText(email) || !Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
            throw new IllegalArgumentException("Your Google account's email address is not verified.");
        }

        String name = jwt.getClaimAsString("name");
        if (!StringUtils.hasText(name)) {
            name = email.substring(0, email.indexOf('@'));
        }
        return new GoogleUser(email, name);
    }

    private static OAuth2TokenValidator<Jwt> claim(java.util.function.Predicate<Jwt> check, String message) {
        return jwt -> check.test(jwt)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", message, null));
    }
}
