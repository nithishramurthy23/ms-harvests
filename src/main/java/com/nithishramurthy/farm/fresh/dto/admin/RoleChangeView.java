package com.nithishramurthy.farm.fresh.dto.admin;

import com.nithishramurthy.farm.fresh.entity.Role;
import com.nithishramurthy.farm.fresh.entity.RoleChange;

import java.time.Instant;

/** One line of the role change log. */
public record RoleChangeView(Long id, String userName, String userEmail, Role fromRole, Role toRole,
                             String changedBy, Instant changedAt) {

    public static RoleChangeView from(RoleChange c) {
        return new RoleChangeView(c.getId(), c.getUser().getName(), c.getUser().getEmail(),
                c.getFromRole(), c.getToRole(), c.getChangedBy(), c.getChangedAt());
    }
}
