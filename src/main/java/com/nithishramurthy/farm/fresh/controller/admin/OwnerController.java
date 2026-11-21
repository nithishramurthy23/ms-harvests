package com.nithishramurthy.farm.fresh.controller.admin;

import com.nithishramurthy.farm.fresh.details.CustomUserDetails;
import com.nithishramurthy.farm.fresh.dto.admin.RoleChangeRequest;
import com.nithishramurthy.farm.fresh.dto.admin.SalesSummary;
import com.nithishramurthy.farm.fresh.dto.admin.UserView;
import com.nithishramurthy.farm.fresh.service.OwnerService;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Owner-only: roles and the sales overview. */
@RestController
@RequestMapping("/api/owner")
public class OwnerController {

    private final OwnerService ownerService;

    public OwnerController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @GetMapping("/users")
    public List<UserView> listUsers() {
        return ownerService.listUsers();
    }

    @PatchMapping("/users/{id}/role")
    public UserView changeRole(@PathVariable Long id, @Valid @RequestBody RoleChangeRequest request,
                               @AuthenticationPrincipal CustomUserDetails me) {
        return ownerService.changeRole(id, request.role(), me.getUser().getId());
    }

    @GetMapping("/summary")
    public SalesSummary summary() {
        return ownerService.summary();
    }
}
