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

    /** All products, including hidden ones, so admins can show them again. */
    @GetMapping("/products")
    public List<AdminProduct> listProducts() {
        return inventoryService.listProducts();
    }

    /** Categories for the drop-down in the product form. */
    @GetMapping("/categories")
    public List<CategoryOption> listCategories() {
        return inventoryService.listCategories();
    }

    /** Adds a product. If it starts with stock, that first stock is written to the stock log too. */
    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminProduct createProduct(@Valid @RequestBody ProductRequest request,
                                      @AuthenticationPrincipal CustomUserDetails me) {
        return inventoryService.createProduct(request, me.getUsername());
    }

    /** Changes name, description, picture, category and price. Stock is not changed here on purpose; it has its own endpoint with a reason. */
    @PutMapping("/products/{id}")
    public AdminProduct updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return inventoryService.updateProduct(id, request);
    }

    /** Hides or shows a product. We never delete products, because old orders still point at them. */
    @PatchMapping("/products/{id}/active")
    public AdminProduct setActive(@PathVariable Long id, @Valid @RequestBody ActiveRequest request) {
        return inventoryService.setActive(id, request.active());
    }

    /**
     * Adds stock (positive change) or removes it (negative). Uses the same single conditional
     * UPDATE as checkout, so it can't push stock below zero even while orders are coming in.
     */
    @PostMapping("/products/{id}/stock")
    public AdminProduct adjustStock(@PathVariable Long id, @Valid @RequestBody StockChangeRequest request,
                                    @AuthenticationPrincipal CustomUserDetails me) {
        return inventoryService.adjustStock(id, request.change(), request.reason(), me.getUsername());
    }

    /** Every stock change for one product: how much, the new total, why, and who did it. */
    @GetMapping("/products/{id}/stock")
    public List<StockMovementView> stockHistory(@PathVariable Long id) {
        return inventoryService.stockHistory(id);
    }
}
