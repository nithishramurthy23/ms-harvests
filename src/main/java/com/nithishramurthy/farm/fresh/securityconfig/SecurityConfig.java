package com.nithishramurthy.farm.fresh.securityconfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.security.web.AuthenticationEntryPoint;
import jakarta.servlet.http.HttpServletResponse;

/** Security setup: stateless JWT auth, which URLs are public, password hashing and CORS. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;

    private final List<String> allowedOrigins;

    public SecurityConfig(CustomUserDetailsService userDetailsService, JwtTokenProvider jwtTokenProvider,
                          @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        this.userDetailsService = userDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.allowedOrigins = allowedOrigins;
    }

    /** bcrypt for passwords: salted and slow on purpose, so stolen hashes are hard to crack. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * How a password login is checked: load the user by email (CustomUserDetailsService),
     * then compare the typed password with the stored hash (passwordEncoder).
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /** Exposes Spring's AuthenticationManager so AuthService can run a password login with it. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * What a request without a valid login gets on a protected URL: 401 Unauthorized.
     * Without this, Spring answers 403 here, and the frontend couldn't tell "please log in"
     * apart from "you're not allowed".
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        };
    }

    /** The filter that reads the JWT on every request (see JwtAuthenticationFilter). */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    /**
     * Public routes are listed here; everything else needs a valid JWT.
     * No server-side sessions: every request carries its own token.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF protection is for cookie logins, where the browser sends the cookie by itself.
            // We use a token the frontend adds to each request by hand, so CSRF doesn't apply.
            .csrf(AbstractHttpConfigurer::disable)

            // Let the React app on another origin (e.g. localhost:5173) call the API.
            // The allowed origins are in corsConfigurationSource() below.
            .cors(withDefaults())

            // Rules are checked top to bottom; the first one that matches wins.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/api/auth/**", "/api/contact", "/api/status", "/actuator/health/**",
                    "/swagger-ui/**", "/v3/api-docs/**",
                    // Razorpay calls the webhook without a login; it's checked by its signature instead.
                    "/api/payments/webhook"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/payments/config").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/*", "/api/offers/today").permitAll()
                // Running the store: products, stock and orders. Users and members can't.
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "OWNER")
                // Sales figures are for the owner only. (Who may give which role is
                // checked in UserManagementService: admins handle members, the owner handles admins.)
                .requestMatchers("/api/owner/**").hasRole("OWNER")
                .anyRequest().authenticated()
            )
            // Never create an HTTP session. Each request proves who it is with its own JWT,
            // which is what lets any backend copy serve any request.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            // Run our JWT filter early, so the user is known before the URL rules are checked.
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint()));
            
        return http.build();
    }

    /** Lets the frontend origins from app.cors.allowed-origins call the API. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(allowedOrigins); // Set via app.cors.allowed-origins
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}