package com.nithishramurthy.farm.fresh.dto.admin;

import com.nithishramurthy.farm.fresh.entity.Role;
import com.nithishramurthy.farm.fresh.entity.User;

import java.util.List;

/**
 * An account on the People screen.
 *
 * @param assignableRoles roles the person viewing may give this account; empty if they can't change it
 */
public record UserView(Long id, String name, String email, Role role, boolean verified, List<Role> assignableRoles) {

    public static UserView from(User u, User viewer) {
        List<Role> assignable = u.getId().equals(viewer.getId())
                ? List.of() // nobody changes their own role
                : viewer.roleEnum().assignableTo(u.roleEnum());
        return new UserView(u.getId(), u.getName(), u.getEmail(), u.roleEnum(), u.isEnabled(), assignable);
    }
}
