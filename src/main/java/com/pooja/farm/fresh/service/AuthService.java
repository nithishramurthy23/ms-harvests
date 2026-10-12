// src/main/java/com/pooja/farm/fresh/service/AuthService.java

package com.pooja.farm.fresh.service;

import com.pooja.farm.fresh.dto.AuthResponse;
import com.pooja.farm.fresh.dto.LoginRequest;
import com.pooja.farm.fresh.dto.UpdateProfileRequest;
import com.pooja.farm.fresh.dto.UserRegistrationRequest;
import com.pooja.farm.fresh.entity.User;
import com.pooja.farm.fresh.repository.UserRepository;
import com.pooja.farm.fresh.securityconfig.CustomUserDetailsService;
import com.pooja.farm.fresh.securityconfig.JwtTokenProvider;

import java.time.Instant;
import java.util.Random;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UserDetails;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;
    private final JwtTokenProvider jwtTokenProvider;
    private final MailService mailService;
    private final CustomUserDetailsService customUserDetailsService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       OtpService otpService,
                       JwtTokenProvider jwtTokenProvider,
                       MailService mailService,
                       CustomUserDetailsService customUserDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.otpService = otpService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.mailService = mailService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Transactional
    public User registerUser(UserRegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        User newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setName(request.getName());
        newUser.setPhone(request.getPhone());
        newUser.setAddress(request.getAddress());
        newUser.setRole("ROLE_USER");
        newUser.setEnabled(false);

        User savedUser = userRepository.save(newUser);

        otpService.generateAndSendOtp(savedUser.getEmail(), "Account Activation OTP", "account activation");

        return savedUser;
    }

    public AuthResponse authenticateUser(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + request.getEmail()));

        if (!user.isEnabled()) {
            otpService.generateAndSendOtp(user.getEmail(), "Account Activation OTP", "account activation");
            throw new IllegalArgumentException("Account not verified. An OTP has been sent to your email for activation.");
        }

        Authentication authentication;
        String jwt;

        if (request.getOtp() != null && !request.getOtp().isEmpty()) {
            // OTP-based login
            if (!otpService.validateOtp(request.getEmail(), request.getOtp())) {
                throw new IllegalArgumentException("Invalid or expired OTP.");
            }
            // Manually create an authenticated token for OTP login
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(request.getEmail());
            authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate JWT token
            jwt = jwtTokenProvider.generateToken(authentication);

        } else if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            // Password-based login
            try {
                authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getEmail(),
                                request.getPassword()
                        )
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                jwt = jwtTokenProvider.generateToken(authentication);
            } catch (org.springframework.security.core.AuthenticationException e) {
                throw new IllegalArgumentException("Invalid email or password.");
            }
        } else {
            throw new IllegalArgumentException("Either password or OTP is required for login.");
        }

        return new AuthResponse(jwt, user);
    }
    
    @Transactional
    public User updateUserProfile(String userEmail, UpdateProfileRequest updateRequest) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        user.setName(updateRequest.getName());
        user.setPhone(updateRequest.getPhone());
        user.setAddress(updateRequest.getAddress());
        
        return userRepository.save(user);
    }
    
    // New method to verify OTP and log in a new user in a single step
    @Transactional
    public AuthResponse verifyAndLogin(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        if (!otpService.validateOtp(email, otp)) {
            throw new IllegalArgumentException("Invalid or expired OTP.");
        }

        user.setEnabled(true);
        userRepository.save(user);

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtTokenProvider.generateToken(authentication);

        return new AuthResponse(jwt, user);
    }


    @Transactional
    public void resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        otpService.generateAndSendOtp(user.getEmail(), "Account Activation OTP", "account activation");
    }

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with email " + email + " not found."));
        otpService.generateAndSendOtp(email, "Password Reset OTP", "password reset");
    }

    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        boolean isValidOtp = otpService.validateOtp(email, otp);
        if (!isValidOtp) {
            throw new IllegalArgumentException("Invalid or expired OTP for password reset.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with email " + email + " not found."));

        if (!isStrongPassword(newPassword)) {
            throw new IllegalArgumentException("Password must contain at least 8 characters, including one uppercase, one lowercase, one digit, and one special character.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    
    public User getUserFromToken(String token) {
        String userEmail = jwtTokenProvider.getUsernameFromJWT(token);
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found for the provided token."));
    }

    private boolean isStrongPassword(String password) {
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*?&]{8,}$";
        return password.matches(regex);
    }
}
