package com.nithishramurthy.farm.fresh.dto.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Add (positive) or remove (negative) units, with a note for the stock history. */
public record StockChangeRequest(
        @NotNull(message = "Change is required") Integer change,
        @Size(max = 255) String reason) {
}
