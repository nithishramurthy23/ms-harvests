package com.nithishramurthy.farm.fresh.controller;

import com.nithishramurthy.farm.fresh.dto.CatalogProduct;
import com.nithishramurthy.farm.fresh.dto.ProductSearchResult;
import com.nithishramurthy.farm.fresh.dto.TodayOffer;
import com.nithishramurthy.farm.fresh.service.ProductSearchService;
import com.nithishramurthy.farm.fresh.service.ProductService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/** Public, read-only catalogue: products with today's price and stock, and today's offer. */
@RestController
@RequestMapping("/api")
public class CatalogController {

    private final ProductService productService;
    private final ProductSearchService searchService;

    public CatalogController(ProductService productService, ProductSearchService searchService) {
        this.productService = productService;
        this.searchService = searchService;
    }

    @GetMapping("/products")
    public List<CatalogProduct> listProducts() {
        return productService.listCatalog();
    }

    /**
     * Search with filters and sorting, one page at a time. Every parameter is optional:
     * {@code /api/products/search?q=carrot&category=vegetables&maxPrice=100&inStock=true&sort=price-asc&page=0}
     */
    @GetMapping("/products/search")
    public ProductSearchResult search(@RequestParam(required = false) String q,
                                      @RequestParam(required = false) String category,
                                      @RequestParam(required = false) BigDecimal minPrice,
                                      @RequestParam(required = false) BigDecimal maxPrice,
                                      @RequestParam(defaultValue = "false") boolean inStock,
                                      @RequestParam(required = false) String sort,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "" + ProductSearchService.DEFAULT_PAGE_SIZE) int size) {
        return searchService.search(new ProductSearchService.Query(q, category, minPrice, maxPrice, inStock,
                ProductSearchService.Sort.from(sort), page, size));
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
