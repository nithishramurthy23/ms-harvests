package com.nithishramurthy.farm.fresh.dto.admin;

import com.nithishramurthy.farm.fresh.entity.Role;
import jakarta.validation.constraints.NotNull;

/** Give a user a different role. */
public record RoleChangeRequest(@NotNull(message = "Role is required") Role role) {
}
