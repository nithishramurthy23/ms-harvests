// src/main/java/com/pooja/farm/fresh/controller/AuthController.java

package com.pooja.farm.fresh.controller;

import com.pooja.farm.fresh.details.CustomUserDetails;
import com.pooja.farm.fresh.dto.AuthResponse;
import com.pooja.farm.fresh.dto.LoginRequest;
import com.pooja.farm.fresh.dto.UpdateProfileRequest;
import com.pooja.farm.fresh.dto.UserRegistrationRequest;
import com.pooja.farm.fresh.dto.SendOtpRequest;
import com.pooja.farm.fresh.dto.VerifyOtpRequest;
import com.pooja.farm.fresh.dto.PasswordResetRequest;
import com.pooja.farm.fresh.dto.RequestPasswordResetRequest;
import com.pooja.farm.fresh.dto.UserProfileResponse;
import com.pooja.farm.fresh.entity.User;
import com.pooja.farm.fresh.service.AuthService;
import com.pooja.farm.fresh.service.OtpService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    public AuthController(AuthService authService, OtpService otpService) {
        this.authService = authService;
        this.otpService = otpService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationRequest registerRequest) {
        try {
            authService.registerUser(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Registration successful. Please check your email for OTP verification."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            AuthResponse authResponse = authService.authenticateUser(loginRequest);
            return ResponseEntity.ok(authResponse);
        } catch (IllegalArgumentException | org.springframework.security.authentication.BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Login failed: " + e.getMessage()));
        }
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody SendOtpRequest sendOtpRequest) {
        try {
            otpService.generateAndSendOtp(sendOtpRequest.getEmail(), "Login Confirmation OTP", "login confirmation");
            return ResponseEntity.ok(Map.of("message", "OTP sent successfully to your email."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to send OTP: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest verifyOtpRequest) {
        try {
            AuthResponse authResponse = authService.verifyAndLogin(verifyOtpRequest.getEmail(), verifyOtpRequest.getOtp());
            return ResponseEntity.ok(authResponse);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "OTP verification failed: " + e.getMessage()));
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@Valid @RequestBody SendOtpRequest resendRequest) {
        try {
            otpService.generateAndSendOtp(resendRequest.getEmail(), "Account Activation OTP", "account activation");
            return ResponseEntity.ok(Map.of("message", "New OTP sent to your email."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to resend OTP: " + e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody RequestPasswordResetRequest request) {
        try {
            authService.forgotPassword(request.getEmail());
            return ResponseEntity.ok(Map.of("message", "If an account exists, an OTP has been sent to your email."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Map.of("message", "If an account exists, an OTP has been sent to your email."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to process forgot password request: " + e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        try {
            authService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password has been reset successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to reset password: " + e.getMessage()));
        }
    }

    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest updateRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String userEmail = userDetails.getUser().getEmail();

            User updatedUser = authService.updateUserProfile(userEmail, updateRequest);
            
            UserProfileResponse profileResponse = new UserProfileResponse(
                updatedUser.getId(),
                updatedUser.getName(),
                updatedUser.getEmail(),
                updatedUser.getPhone(),
                updatedUser.getAddress()
            );

            return ResponseEntity.ok(profileResponse);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to update user profile: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            User user = authService.getUserFromToken(token);

            UserProfileResponse profile = new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getAddress()
            );

            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to get user profile: " + e.getMessage()));
        }
    }
}
