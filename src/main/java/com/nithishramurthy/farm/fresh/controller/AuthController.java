// src/main/java/com/nithishramurthy/farm/fresh/controller/AuthController.java

package com.nithishramurthy.farm.fresh.controller;

import com.nithishramurthy.farm.fresh.details.CustomUserDetails;
import com.nithishramurthy.farm.fresh.dto.AuthResponse;
import com.nithishramurthy.farm.fresh.dto.GoogleLoginRequest;
import com.nithishramurthy.farm.fresh.dto.LoginRequest;
import com.nithishramurthy.farm.fresh.dto.UpdateProfileRequest;
import com.nithishramurthy.farm.fresh.dto.UserRegistrationRequest;
import com.nithishramurthy.farm.fresh.dto.SendOtpRequest;
import com.nithishramurthy.farm.fresh.dto.VerifyOtpRequest;
import com.nithishramurthy.farm.fresh.dto.PasswordResetRequest;
import com.nithishramurthy.farm.fresh.dto.RequestPasswordResetRequest;
import com.nithishramurthy.farm.fresh.dto.UserProfileResponse;
import com.nithishramurthy.farm.fresh.entity.User;
import com.nithishramurthy.farm.fresh.securityconfig.GoogleTokenVerifier;
import com.nithishramurthy.farm.fresh.service.AuthService;
import com.nithishramurthy.farm.fresh.service.OtpService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** REST endpoints for sign-up, login (password or email OTP), password reset and the user's profile. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthController(AuthService authService, OtpService otpService, GoogleTokenVerifier googleTokenVerifier) {
        this.authService = authService;
        this.otpService = otpService;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    /** Creates a disabled account and emails an activation OTP. */
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

    /** Logs in with a password or an OTP and returns a JWT. */
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

    /**
     * Tells the frontend whether to show "Sign in with Google" and which client ID
     * to use. The client ID is public (it's in every Google button on the web), and
     * serving it from here means GOOGLE_CLIENT_ID only has to be set in one place.
     */
    @GetMapping("/google/config")
    public ResponseEntity<?> googleConfig() {
        return ResponseEntity.ok(Map.of(
                "enabled", googleTokenVerifier.isEnabled(),
                "clientId", googleTokenVerifier.getClientId()));
    }

    /** Sign in with Google: swaps a Google ID token for our JWT. */
    @PostMapping("/google")
    public ResponseEntity<?> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        try {
            return ResponseEntity.ok(authService.loginWithGoogle(request.getCredential()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Google sign-in failed: " + e.getMessage()));
        }
    }

    /** Emails an OTP for passwordless login. */
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

    /** Activates a new account with its OTP and logs the user in. */
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

    /** Sends a fresh activation OTP. */
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

    /** Emails a password-reset OTP. Always answers the same way so it can't be used to find out which emails are registered. */
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

    /** Sets a new password after checking the reset OTP. */
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

    /** Updates name, phone and address of the logged-in user. */
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

    /** Returns the profile of the user the token belongs to. */
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
