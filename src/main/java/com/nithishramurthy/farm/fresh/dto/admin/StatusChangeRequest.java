package com.nithishramurthy.farm.fresh.dto.admin;

import com.nithishramurthy.farm.fresh.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

/** Move an order to the next status. */
public record StatusChangeRequest(@NotNull(message = "Status is required") OrderStatus status) {
}
