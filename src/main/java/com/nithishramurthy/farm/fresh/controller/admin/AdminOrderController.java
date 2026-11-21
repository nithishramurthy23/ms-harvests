package com.nithishramurthy.farm.fresh.controller.admin;

import com.nithishramurthy.farm.fresh.details.CustomUserDetails;
import com.nithishramurthy.farm.fresh.dto.admin.AdminOrderView;
import com.nithishramurthy.farm.fresh.dto.admin.StatusChangeRequest;
import com.nithishramurthy.farm.fresh.entity.OrderStatus;
import com.nithishramurthy.farm.fresh.entity.PaymentStatus;
import com.nithishramurthy.farm.fresh.payment.PaymentService;
import com.nithishramurthy.farm.fresh.service.OrderFulfilmentService;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Every customer's orders and their status, for admins and owners. */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderFulfilmentService fulfilmentService;
    private final PaymentService paymentService;

    public AdminOrderController(OrderFulfilmentService fulfilmentService, PaymentService paymentService) {
        this.fulfilmentService = fulfilmentService;
        this.paymentService = paymentService;
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

    /** Tries a failed refund again. Refunds normally happen by themselves when a paid order is cancelled. */
    @PostMapping("/{id}/refund")
    public Map<String, PaymentStatus> retryRefund(@PathVariable Long id) {
        return Map.of("paymentStatus", paymentService.refund(id));
    }
}
