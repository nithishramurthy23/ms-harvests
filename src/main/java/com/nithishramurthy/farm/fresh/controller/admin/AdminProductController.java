package com.nithishramurthy.farm.fresh.controller.admin;

import com.nithishramurthy.farm.fresh.details.CustomUserDetails;
import com.nithishramurthy.farm.fresh.dto.admin.ActiveRequest;
import com.nithishramurthy.farm.fresh.dto.admin.AdminProduct;
import com.nithishramurthy.farm.fresh.dto.admin.CategoryOption;
import com.nithishramurthy.farm.fresh.dto.admin.ProductRequest;
import com.nithishramurthy.farm.fresh.dto.admin.StockChangeRequest;
import com.nithishramurthy.farm.fresh.dto.admin.StockMovementView;
import com.nithishramurthy.farm.fresh.service.InventoryService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Products and stock, for admins and owners (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminProductController {

    private final InventoryService inventoryService;

    public AdminProductController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/products")
    public List<AdminProduct> listProducts() {
        return inventoryService.listProducts();
    }

    @GetMapping("/categories")
    public List<CategoryOption> listCategories() {
        return inventoryService.listCategories();
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminProduct createProduct(@Valid @RequestBody ProductRequest request,
                                      @AuthenticationPrincipal CustomUserDetails me) {
        return inventoryService.createProduct(request, me.getUsername());
    }

    @PutMapping("/products/{id}")
    public AdminProduct updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return inventoryService.updateProduct(id, request);
    }

    @PatchMapping("/products/{id}/active")
    public AdminProduct setActive(@PathVariable Long id, @Valid @RequestBody ActiveRequest request) {
        return inventoryService.setActive(id, request.active());
    }

    @PostMapping("/products/{id}/stock")
    public AdminProduct adjustStock(@PathVariable Long id, @Valid @RequestBody StockChangeRequest request,
                                    @AuthenticationPrincipal CustomUserDetails me) {
        return inventoryService.adjustStock(id, request.change(), request.reason(), me.getUsername());
    }

    @GetMapping("/products/{id}/stock")
    public List<StockMovementView> stockHistory(@PathVariable Long id) {
        return inventoryService.stockHistory(id);
    }
}
