// src/main/java/com/nithishramurthy/farm/fresh/entity/User.java

package com.nithishramurthy.farm.fresh.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;

/** A customer account, including the pending OTP and failed OTP attempts. */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String role; // e.g., "ROLE_USER", "ROLE_ADMIN"

    @Column
    private String address;

    @Column
    private String phone;

    // Optional: for account activation via OTP
    private String otpCode;
    // CHANGE: Use Instant for timezone-agnostic storage
    private Instant otpExpiry; 
    private boolean enabled; // To indicate if the user account is active/verified

    @Column(name = "failed_attempts")
    private int failedAttempts = 0;

    // Limits how often a code can be emailed to this address (OtpService). Stored
    // in the database, not in memory, so the limit holds across every backend copy.
    @Column(name = "otp_last_sent_at")
    private Instant otpLastSentAt;

    @Column(name = "otp_window_started_at")
    private Instant otpWindowStartedAt;

    @Column(name = "otp_sends_in_window")
    private Integer otpSendsInWindow;
    // Constructors
    public User() {}

    public User(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = "ROLE_USER"; // Default role for new users
        this.enabled = false; // User is not enabled until OTP verification
    }

    public User(Long id, String name, String email, String address, String phone, int failed_attempts){
        this.id = id;
        this.name = name;
        this.email = email;
        this.address = address;
        this.phone = phone;
        this.role = "ROLE_USER";
        this.enabled = false;
        this.failedAttempts=failed_attempts;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    /** The role column as an enum. Unknown or old values are read as USER. */
    public Role roleEnum() {
        return Role.fromAuthority(role);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    // CHANGE: Getter and Setter for Instant type
    public Instant getOtpExpiry() {
        return otpExpiry;
    }

    public void setOtpExpiry(Instant otpExpiry) {
        this.otpExpiry = otpExpiry;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getOtpLastSentAt() {
        return otpLastSentAt;
    }

    public void setOtpLastSentAt(Instant otpLastSentAt) {
        this.otpLastSentAt = otpLastSentAt;
    }

    public Instant getOtpWindowStartedAt() {
        return otpWindowStartedAt;
    }

    public void setOtpWindowStartedAt(Instant otpWindowStartedAt) {
        this.otpWindowStartedAt = otpWindowStartedAt;
    }

    public int getOtpSendsInWindow() {
        return otpSendsInWindow == null ? 0 : otpSendsInWindow;
    }

    public void setOtpSendsInWindow(int otpSendsInWindow) {
        this.otpSendsInWindow = otpSendsInWindow;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

   
}
