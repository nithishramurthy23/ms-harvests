package com.nithishramurthy.farm.fresh.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Body for changing a password while logged in. Not used by any endpoint yet. */
@Data
public class UpdatePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    private String newPassword;
}

