package com.nithishramurthy.farm.fresh.controller.admin;

import com.nithishramurthy.farm.fresh.details.CustomUserDetails;
import com.nithishramurthy.farm.fresh.dto.admin.RoleChangeRequest;
import com.nithishramurthy.farm.fresh.dto.admin.RoleChangeView;
import com.nithishramurthy.farm.fresh.dto.admin.UserView;
import com.nithishramurthy.farm.fresh.service.UserManagementService;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * People and roles, for admins and the owner. What each of them may change
 * is decided in UserManagementService (admins: user/member only).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    private final UserManagementService userManagementService;

    public AdminUserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    /**
     * Everyone, optionally filtered by name or email (?q=). Each person comes with the roles
     * the caller may give them, so the dashboard only shows choices the server will accept.
     */
    @GetMapping("/users")
    public List<UserView> listUsers(@RequestParam(required = false) String q,
                                    @AuthenticationPrincipal CustomUserDetails me) {
        return userManagementService.listUsers(me.getUser(), q);
    }

    /** Changes someone's role. UserManagementService refuses moves the caller isn't allowed to make (403). */
    @PatchMapping("/users/{id}/role")
    public UserView changeRole(@PathVariable Long id, @Valid @RequestBody RoleChangeRequest request,
                               @AuthenticationPrincipal CustomUserDetails me) {
        return userManagementService.changeRole(id, request.role(), me.getUser());
    }

    /** The last 50 role changes, newest first: who was changed, from what, to what, and by whom. */
    @GetMapping("/role-changes")
    public List<RoleChangeView> recentRoleChanges() {
        return userManagementService.recentChanges();
    }
}
