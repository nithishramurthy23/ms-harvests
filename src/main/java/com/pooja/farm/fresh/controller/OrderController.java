// src/main/java/com/pooja/farm/fresh/controller/OrderController.java

package com.pooja.farm.fresh.controller;

import com.pooja.farm.fresh.details.CustomUserDetails;
import com.pooja.farm.fresh.dto.OrderDTO;
import com.pooja.farm.fresh.dto.OrderRequest;
import com.pooja.farm.fresh.dto.OrderResponse;
import com.pooja.farm.fresh.dto.SendOtpRequest;
import com.pooja.farm.fresh.service.OrderService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/initiate-order")
    public ResponseEntity<?> initiateOrder(@Valid @RequestBody SendOtpRequest sendOtpRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            if (!userEmail.equals(sendOtpRequest.getEmail())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Cannot request OTP for another user's order."));
            }

            orderService.initiateOrderConfirmation(userEmail);
            return ResponseEntity.ok(Map.of("message", "OTP sent to your email for order confirmation."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Failed to send OTP for order confirmation."));
        }
    }

    @PostMapping("/place-order")
    public ResponseEntity<?> placeOrder(@Valid @RequestBody OrderRequest orderRequest, BindingResult bindingResult) {
        try {
            if (bindingResult.hasErrors()) {
                Map<String, String> errors = new HashMap<>();
                bindingResult.getFieldErrors()
                        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
                return ResponseEntity.badRequest().body(errors);
            }

            OrderResponse response = orderService.placeOrder(orderRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Failed to place order."));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<OrderDTO>> getOrderHistory() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();

        List<OrderDTO> orderHistory = orderService.getOrdersByUserId(userId).stream()
                .map(OrderDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(orderHistory);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDTO> getOrderDetails(@PathVariable Long orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUser().getId();

        OrderDTO orderDetails = orderService.getOrderById(orderId);
        if (!orderDetails.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        return ResponseEntity.ok(orderDetails);
    }
}
