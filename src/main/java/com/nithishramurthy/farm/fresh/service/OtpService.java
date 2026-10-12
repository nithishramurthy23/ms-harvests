// src/main/java/com/nithishramurthy/farm/fresh/service/OtpService.java

package com.nithishramurthy.farm.fresh.service;

import com.nithishramurthy.farm.fresh.entity.User;
import com.nithishramurthy.farm.fresh.mail.MailConstant;
import com.nithishramurthy.farm.fresh.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OtpService {

    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

    private final UserRepository userRepository;
    private final MailService mailService;

    public OtpService(UserRepository userRepository, MailService mailService) {
        this.userRepository = userRepository;
        this.mailService = mailService;
    }

    /**
     * Generates an OTP, saves it to the user, and sends it via email.
     *
     * @param email The email address to send the OTP to.
     * @param subject The subject of the OTP email.
     * @param purpose A string indicating the purpose (e.g., "account verification", "login", "order confirmation").
     * @throws IllegalArgumentException if the user is not found.
     */
   @Transactional
    public void generateAndSendOtp(String email, String subject, String purpose) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("User with email " + email + " not found.");
        }

        User user = userOptional.get();
        String otp = generateOtp();
        // CHANGE: Use Instant for timezone-agnostic expiry
        Instant expiryTime = Instant.now().plus(Duration.ofMinutes(5)); 

        user.setOtpCode(otp);
        user.setOtpExpiry(expiryTime);
        userRepository.save(user);

        String mailBody = String.format("Dear %s,\n\nYour One-Time Password (OTP) for %s is: %s\n\nThis OTP is valid for 5 minutes.\n\nDo not share this OTP with anyone.\n\nThank you,\nFarm Fresh Team", user.getName(), purpose, otp);
        mailService.sendTestMail(email, subject, mailBody);
    }


    /**
     * Generates a random 6-digit OTP.
     */
    private String generateOtp() {
        int otp = 100000 + SECURE_RANDOM.nextInt(900000); // Generates a 6-digit number
        return String.valueOf(otp);
    }

    /**
     * Validates the provided OTP for a given user.
     *
     * @param email The user's email.
     * @param otp   The OTP provided by the user.
     * @return true if the OTP is valid and not expired, false otherwise.
     * @throws IllegalArgumentException if the user is not found.
     */
   @Transactional
    public boolean validateOtp(String email, String otp) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("User with email " + email + " not found.");
        }

        User user = userOptional.get();
        Instant nowUtc = Instant.now();

        // Check if maximum attempts are exceeded
        if (user.getFailedAttempts() >= 2) {
            throw new IllegalArgumentException("Maximum OTP attempts (2) exceeded. Please try registering again.");
        }

        // Validate OTP
        if (user.getOtpCode() == null || !user.getOtpCode().equals(otp) || user.getOtpExpiry() == null || nowUtc.isAfter(user.getOtpExpiry())) {
            // Increment failed attempts for invalid OTP
            user.setFailedAttempts(user.getFailedAttempts() + 1);
            user.setOtpCode(null); // Clear OTP on failure to prevent reuse
            user.setOtpExpiry(null);
            userRepository.save(user);
            return false;
        }

        // OTP is valid, clear it and reset failed attempts
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        user.setFailedAttempts(0);
        userRepository.save(user);
        return true;
    }
    /**
     * Marks a user account as enabled.
     * @param email The email of the user to enable.
     */
    @Transactional
    public void enableUserAccount(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("User with email " + email + " not found.");
        }
        User user = userOptional.get();
        user.setEnabled(true);
        userRepository.save(user);
    }

    // Method to check if an account is enabled
    public boolean isAccountEnabled(String email) {
        return userRepository.findByEmail(email)
                .map(User::isEnabled)
                .orElse(false);
    }
}
