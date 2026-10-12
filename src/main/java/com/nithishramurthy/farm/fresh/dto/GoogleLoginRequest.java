package com.nithishramurthy.farm.fresh.dto;

import jakarta.validation.constraints.NotBlank;

/** Body of POST /api/auth/google: the ID token ("credential") from the Google button. */
public class GoogleLoginRequest {

    @NotBlank(message = "Google credential is required")
    private String credential;

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }
}
