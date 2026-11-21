package com.nithishramurthy.farm.fresh.dto.admin;

import jakarta.validation.constraints.NotNull;

/** Show (true) or hide (false) a product in the shop. */
public record ActiveRequest(@NotNull Boolean active) {
}
