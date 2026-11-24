package com.nithishramurthy.farm.fresh.service;

import com.nithishramurthy.farm.fresh.dto.admin.AdminProduct;
import com.nithishramurthy.farm.fresh.dto.admin.CategoryOption;
import com.nithishramurthy.farm.fresh.dto.admin.ProductRequest;
import com.nithishramurthy.farm.fresh.dto.admin.StockMovementView;
import com.nithishramurthy.farm.fresh.entity.Category;
import com.nithishramurthy.farm.fresh.entity.Product;
import com.nithishramurthy.farm.fresh.entity.StockMovement;
import com.nithishramurthy.farm.fresh.exception.ResourceNotFoundException;
import com.nithishramurthy.farm.fresh.repository.CategoryRepository;
import com.nithishramurthy.farm.fresh.repository.ProductRepository;
import com.nithishramurthy.farm.fresh.repository.StockMovementRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** What admins and owners do with products: add, edit, hide, and change stock. */
@Service
public class InventoryService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StockMovementRepository stockMovementRepository;

    public InventoryService(ProductRepository productRepository, CategoryRepository categoryRepository,
                            StockMovementRepository stockMovementRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    /** Every product, hidden ones included. */
    @Transactional(readOnly = true)
    public List<AdminProduct> listProducts() {
        return productRepository.findAll().stream()
                .sorted(Comparator.comparing(Product::getId))
                .map(AdminProduct::from)
                .toList();
    }

    /** Categories for the product form, in id order. */
    @Transactional(readOnly = true)
    public List<CategoryOption> listCategories() {
        return categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(Category::getId))
                .map(c -> new CategoryOption(c.getId(), c.getName(), c.slug()))
                .toList();
    }

    /** Saves a new product. Starting stock is logged as "Initial stock" so the history adds up from day one. */
    @Transactional
    public AdminProduct createProduct(ProductRequest request, String changedBy) {
        Product product = new Product();
        applyDetails(product, request);
        int initialStock = request.initialStock() == null ? 0 : request.initialStock();
        product.setStockQuantity(initialStock);
        product = productRepository.save(product);

        if (initialStock > 0) {
            stockMovementRepository.save(new StockMovement(product, initialStock, initialStock, "Initial stock", changedBy));
        }
        return AdminProduct.from(product);
    }

    /** Changes name, description, picture, category and price. Stock has its own endpoint. */
    @Transactional
    public AdminProduct updateProduct(Long id, ProductRequest request) {
        Product product = find(id);
        applyDetails(product, request);
        return AdminProduct.from(productRepository.save(product));
    }

    /** Hidden products disappear from the shop but stay linked to old orders. */
    @Transactional
    public AdminProduct setActive(Long id, boolean active) {
        Product product = find(id);
        product.setActive(active);
        return AdminProduct.from(productRepository.save(product));
    }

    /**
     * Adds or removes stock and records who did it and why. Uses one conditional
     * UPDATE, like checkout does, so it's safe while orders are coming in.
     *
     * @throws IllegalStateException if removing more than is in stock
     */
    @Transactional
    public AdminProduct adjustStock(Long id, int change, String reason, String changedBy) {
        if (change == 0) {
            throw new IllegalArgumentException("Change must not be 0.");
        }
        Product before = find(id);
        if (productRepository.adjustStock(id, change) == 0) {
            throw new IllegalStateException("Only " + before.getStockQuantity() + " in stock, can't remove " + (-change) + ".");
        }
        Product after = find(id);
        stockMovementRepository.save(new StockMovement(after, change, after.getStockQuantity(),
                reason == null || reason.isBlank() ? (change > 0 ? "Restock" : "Correction") : reason.trim(),
                changedBy));
        return AdminProduct.from(after);
    }

    /** Stock changes for one product, newest first. find(id) first, so an unknown id is a 404, not an empty list. */
    @Transactional(readOnly = true)
    public List<StockMovementView> stockHistory(Long id) {
        find(id);
        return stockMovementRepository.findByProductIdOrderByChangedAtDescIdDesc(id).stream()
                .map(StockMovementView::from)
                .toList();
    }

    private Product find(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    /** Copies the form fields onto the product. Shared by create and update. */
    private void applyDetails(Product product, ProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown category: " + request.categoryId()));
        product.setName(request.name().trim());
        product.setDescription(blankToNull(request.description()));
        product.setWeight(blankToNull(request.weight()));
        product.setImageUrl(blankToNull(request.imageUrl()));
        product.setCategory(category);
        // price is the everyday price; discounts are applied on top of original_price
        product.setPrice(request.price());
        product.setOriginalPrice(request.price());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
