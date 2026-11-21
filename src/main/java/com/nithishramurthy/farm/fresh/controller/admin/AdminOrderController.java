package com.nithishramurthy.farm.fresh.controller.admin;

import com.nithishramurthy.farm.fresh.details.CustomUserDetails;
import com.nithishramurthy.farm.fresh.dto.admin.AdminOrderView;
import com.nithishramurthy.farm.fresh.dto.admin.StatusChangeRequest;
import com.nithishramurthy.farm.fresh.entity.OrderStatus;
import com.nithishramurthy.farm.fresh.service.OrderFulfilmentService;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Every customer's orders and their status, for admins and owners. */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderFulfilmentService fulfilmentService;

    public AdminOrderController(OrderFulfilmentService fulfilmentService) {
        this.fulfilmentService = fulfilmentService;
    }

    @GetMapping
    public List<AdminOrderView> listOrders(@RequestParam(required = false) OrderStatus status) {
        return fulfilmentService.listOrders(status);
    }

    @PatchMapping("/{id}/status")
    public AdminOrderView changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest request,
                                       @AuthenticationPrincipal CustomUserDetails me) {
        return fulfilmentService.changeStatus(id, request.status(), me.getUsername());
    }
}
