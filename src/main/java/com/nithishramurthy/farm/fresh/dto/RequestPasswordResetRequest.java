package com.nithishramurthy.farm.fresh.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Body for "forgot password": just the email to send the reset OTP to. */
public class RequestPasswordResetRequest {

    @Email
    @NotBlank
    private String email;

    // Getter & Setter
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

