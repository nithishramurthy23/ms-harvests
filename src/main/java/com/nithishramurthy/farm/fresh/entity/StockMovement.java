package com.nithishramurthy.farm.fresh.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * One change to a product's stock: a restock, a correction, or stock put back
 * by a cancelled order. Sales themselves are recorded as orders, not here.
 */
@Entity
@Table(name = "stock_movements", indexes = @Index(name = "idx_stock_movements_product_id", columnList = "product_id"))
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Units added (positive) or removed (negative). */
    @Column(name = "quantity_change", nullable = false)
    private int change;

    @Column(name = "stock_after", nullable = false)
    private int stockAfter;

    @Column(length = 255)
    private String reason;

    /** Email of the admin who made the change, or "system". */
    @Column(name = "changed_by", length = 255, nullable = false)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt = Instant.now();

    protected StockMovement() {}

    public StockMovement(Product product, int change, int stockAfter, String reason, String changedBy) {
        this.product = product;
        this.change = change;
        this.stockAfter = stockAfter;
        this.reason = reason;
        this.changedBy = changedBy;
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public int getChange() { return change; }
    public int getStockAfter() { return stockAfter; }
    public String getReason() { return reason; }
    public String getChangedBy() { return changedBy; }
    public Instant getChangedAt() { return changedAt; }
}
