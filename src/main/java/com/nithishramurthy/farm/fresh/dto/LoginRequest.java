package com.nithishramurthy.farm.fresh.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Login body. Send either a password or an OTP along with the email. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    private String email;

    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password; // Optional for OTP login

    private String otp; // Optional for password login

    // Default constructor
    public LoginRequest() {}

    // Constructor for password-based login
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // Constructor for OTP-based login
    public static LoginRequest withOtp(String email, String otp) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setOtp(otp);
        return request;
    }

    // Getters and Setters
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

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    @Override
    public String toString() {
        return "LoginRequest{" +
                "email='" + email + '\'' +
                ", password=" + (password != null ? "[PROTECTED]" : "null") +
                ", otp=" + (otp != null ? "[PROVIDED]" : "null") +
                '}';
    }
}
