package com.pooja.farm.fresh.entity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;



import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.DayOfWeek;

@Entity
@Table(name = "discount_rules", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"day_of_week", "category_id"})
})
public class DiscountRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(BigDecimal discountPercent) {
        this.discountPercent = discountPercent;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "DiscountRule [id=" + id + ", dayOfWeek=" + dayOfWeek + ", discountPercent=" + discountPercent
                + ", active=" + active + ", category=" + category + "]";
    }



    
}
