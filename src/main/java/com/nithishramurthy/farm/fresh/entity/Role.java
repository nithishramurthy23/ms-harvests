package com.nithishramurthy.farm.fresh.entity;

import java.util.Arrays;

/**
 * What an account is allowed to do. Stored in users.role as the Spring Security
 * authority ("ROLE_USER", ...), so existing rows keep working.
 *
 * USER and MEMBER shop. ADMIN also runs the store (products, stock, orders).
 * OWNER can do everything an admin can, plus change people's roles and see sales.
 */
public enum Role {
    USER,
    MEMBER,
    ADMIN,
    OWNER;

    public String authority() {
        return "ROLE_" + name();
    }

    public boolean canManageStore() {
        return this == ADMIN || this == OWNER;
    }

    /** Reads a stored authority; anything unknown is treated as a plain user. */
    public static Role fromAuthority(String authority) {
        return Arrays.stream(values())
                .filter(role -> role.authority().equals(authority))
                .findFirst()
                .orElse(USER);
    }
}
