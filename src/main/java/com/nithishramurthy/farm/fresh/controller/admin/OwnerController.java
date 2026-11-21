package com.nithishramurthy.farm.fresh.controller.admin;

import com.nithishramurthy.farm.fresh.dto.admin.SalesSummary;
import com.nithishramurthy.farm.fresh.service.OwnerService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Owner-only: the sales overview. Roles are managed through AdminUserController. */
@RestController
@RequestMapping("/api/owner")
public class OwnerController {

    private final OwnerService ownerService;

    public OwnerController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @GetMapping("/summary")
    public SalesSummary summary() {
        return ownerService.summary();
    }
}
