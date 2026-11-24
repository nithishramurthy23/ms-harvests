package com.nithishramurthy.farm.fresh.entity;

import java.util.Arrays;
import java.util.List;

/**
 * What an account is allowed to do. Stored in users.role as the Spring Security
 * authority ("ROLE_USER", ...), so existing rows keep working.
 *
 * USER and MEMBER shop. ADMIN also runs the store (products, stock, orders)
 * and decides who is a member. OWNER can do everything an admin can, decides
 * who is an admin or owner, and sees sales.
 */
public enum Role {
    USER,
    MEMBER,
    ADMIN,
    OWNER;

    /** The name Spring Security expects: hasRole("ADMIN") looks for the authority "ROLE_ADMIN". */
    public String authority() {
        return "ROLE_" + name();
    }

    /** True for the two roles that can open the store dashboard. */
    public boolean canManageStore() {
        return this == ADMIN || this == OWNER;
    }

    /**
     * Roles this role may give to someone who currently has {@code current}.
     * Owners manage everyone; admins only move people between USER and MEMBER.
     * Empty means "can't change this person's role at all".
     */
    public List<Role> assignableTo(Role current) {
        return switch (this) {
            case OWNER -> List.of(values());
            case ADMIN -> (current == USER || current == MEMBER) ? List.of(USER, MEMBER) : List.of();
            case USER, MEMBER -> List.of();
        };
    }

    /** Whether this role may change someone from {@code current} to {@code next}. */
    public boolean canChange(Role current, Role next) {
        return assignableTo(current).contains(next);
    }

    /** Reads a stored authority; anything unknown is treated as a plain user. */
    public static Role fromAuthority(String authority) {
        return Arrays.stream(values())
                .filter(role -> role.authority().equals(authority))
                .findFirst()
                .orElse(USER);
    }
}
