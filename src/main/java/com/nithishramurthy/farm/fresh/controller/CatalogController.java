package com.nithishramurthy.farm.fresh.controller;

import com.nithishramurthy.farm.fresh.dto.CatalogProduct;
import com.nithishramurthy.farm.fresh.dto.TodayOffer;
import com.nithishramurthy.farm.fresh.service.ProductService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public, read-only catalogue: products with today's price and stock, and today's offer. */
@RestController
@RequestMapping("/api")
public class CatalogController {

    private final ProductService productService;

    public CatalogController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public List<CatalogProduct> listProducts() {
        return productService.listCatalog();
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<CatalogProduct> getProduct(@PathVariable Long id) {
        return ResponseEntity.of(productService.findCatalogProduct(id));
    }

    /** 200 with the offer, or 204 when there's no offer today. */
    @GetMapping("/offers/today")
    public ResponseEntity<TodayOffer> todayOffer() {
        return productService.todayOffer()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
