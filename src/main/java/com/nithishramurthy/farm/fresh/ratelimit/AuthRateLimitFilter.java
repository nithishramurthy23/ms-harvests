package com.nithishramurthy.farm.fresh.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;

/**
 * Limits how often one IP address can hit the login and code endpoints, before
 * the request reaches any controller. Answers 429 Too Many Requests with a
 * Retry-After header.
 *
 * <ul>
 *   <li>Sending codes (send-otp, resend-otp, forgot-password, register): 10 per 10 minutes.
 *       These send real emails, so they get the tightest limit.</li>
 *   <li>Checking passwords and codes (login, verify-otp, reset-password, google): 30 per 10 minutes.</li>
 * </ul>
 *
 * The client's IP comes from {@code getRemoteAddr()}. With
 * {@code server.forward-headers-strategy=native}, Tomcat takes it from nginx's
 * X-Forwarded-For, trusting only proxies on private networks, so a client can't
 * fake it by sending that header itself.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    static final Duration WINDOW = Duration.ofMinutes(10);

    private final FixedWindowRateLimiter limiter;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int sendLimit;
    private final int checkLimit;

    public AuthRateLimitFilter(ObjectMapper objectMapper,
                               @Value("${app.rate-limit.enabled:true}") boolean enabled,
                               @Value("${app.rate-limit.send-per-10-min:10}") int sendLimit,
                               @Value("${app.rate-limit.check-per-10-min:30}") int checkLimit) {
        this.limiter = new FixedWindowRateLimiter(Clock.systemUTC());
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.sendLimit = sendLimit;
        this.checkLimit = checkLimit;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !"POST".equals(request.getMethod()) || bucket(request.getRequestURI()) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String bucket = bucket(request.getRequestURI());
        int limit = bucket.equals("send") ? sendLimit : checkLimit;
        long retryAfter = limiter.tryAcquire(bucket + ":" + request.getRemoteAddr(), limit, WINDOW);
        if (retryAfter == 0) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of("message",
                "Too many attempts from your network. Please try again in " + Math.max(1, retryAfter / 60) + " minutes."));
    }

    /** "send" for endpoints that email a code, "check" for ones that check a secret, null for the rest. */
    static String bucket(String path) {
        return switch (path) {
            case "/api/auth/send-otp", "/api/auth/resend-otp", "/api/auth/forgot-password", "/api/auth/register" -> "send";
            case "/api/auth/login", "/api/auth/verify-otp", "/api/auth/reset-password", "/api/auth/google" -> "check";
            default -> null;
        };
    }

    @Scheduled(fixedDelay = 600_000)
    void cleanUp() {
        limiter.forgetOlderThan(WINDOW);
    }
}
