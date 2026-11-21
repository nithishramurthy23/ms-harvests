package com.nithishramurthy.farm.fresh.service;

import com.nithishramurthy.farm.fresh.entity.User;
import com.nithishramurthy.farm.fresh.exception.TooManyRequestsException;
import com.nithishramurthy.farm.fresh.mail.MailConstant;
import com.nithishramurthy.farm.fresh.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Creates, emails and checks one-time passwords (OTPs).
 *
 * Sending is limited per email address: one code a minute and at most five an
 * hour. Each code allows two wrong tries, so someone guessing gets at most ten
 * tries an hour at a 6-digit code (about 1 in 90,000). Without the limit, asking
 * for a new code reset the tries, so guesses were unlimited.
 *
 * Using a code correctly clears the limit, because whoever did it has the inbox.
 * So a customer who signs up, enters the code and goes straight to checkout isn't
 * told to wait.
 */
@Service
public class OtpService {

    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    static final Duration SEND_WINDOW = Duration.ofHours(1);
    static final int MAX_SENDS_PER_WINDOW = 5;

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
     * @throws TooManyRequestsException if a code was sent too recently, or too many this hour
     */
    @Transactional
    public void generateAndSendOtp(String email, String subject, String purpose) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("User with email " + email + " not found.");
        }

        User user = userOptional.get();
        Instant now = Instant.now();
        checkSendLimit(user, now);

        String otp = generateOtp();
        Instant expiryTime = now.plus(Duration.ofMinutes(5));

        user.setOtpCode(otp);
        user.setOtpExpiry(expiryTime);
        user.setFailedAttempts(0); // a new code gets a fresh set of attempts (the send limit caps how many)
        user.setOtpLastSentAt(now);
        user.setOtpSendsInWindow(user.getOtpSendsInWindow() + 1);
        userRepository.save(user);

        String mailBody = String.format("Dear %s,\n\nYour One-Time Password (OTP) for %s is: %s\n\nThis OTP is valid for 5 minutes.\n\nDo not share this OTP with anyone.\n\nThank you,\nFarm Fresh Team", user.getName(), purpose, otp);
        mailService.sendTestMail(email, subject, mailBody);
    }


    /** Refuses a new code if one went out less than a minute ago, or five already went out this hour. */
    private static void checkSendLimit(User user, Instant now) {
        Instant last = user.getOtpLastSentAt();
        if (last != null && now.isBefore(last.plus(RESEND_COOLDOWN))) {
            long wait = Duration.between(now, last.plus(RESEND_COOLDOWN)).toSeconds() + 1;
            throw new TooManyRequestsException("Please wait " + wait + " seconds before asking for another code.", wait);
        }
        Instant windowStart = user.getOtpWindowStartedAt();
        if (windowStart == null || !now.isBefore(windowStart.plus(SEND_WINDOW))) {
            user.setOtpWindowStartedAt(now); // a new hour starts with this code
            user.setOtpSendsInWindow(0);
        } else if (user.getOtpSendsInWindow() >= MAX_SENDS_PER_WINDOW) {
            long wait = Duration.between(now, windowStart.plus(SEND_WINDOW)).toSeconds() + 1;
            throw new TooManyRequestsException("Too many codes requested. Please try again in "
                    + Math.max(1, wait / 60) + " minutes.", wait);
        }
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
            throw new IllegalArgumentException("Maximum OTP attempts (2) exceeded. Please request a new OTP.");
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

        // OTP is valid, clear it and reset failed attempts and the send limit
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        user.setFailedAttempts(0);
        user.setOtpLastSentAt(null);
        user.setOtpWindowStartedAt(null);
        user.setOtpSendsInWindow(0);
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
