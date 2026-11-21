package com.nithishramurthy.farm.fresh.dto.admin;

import com.nithishramurthy.farm.fresh.entity.Role;
import com.nithishramurthy.farm.fresh.entity.User;

/** An account as the owner sees it on the roles screen. */
public record UserView(Long id, String name, String email, Role role, boolean verified) {

    public static UserView from(User u) {
        return new UserView(u.getId(), u.getName(), u.getEmail(), u.roleEnum(), u.isEnabled());
    }
}
