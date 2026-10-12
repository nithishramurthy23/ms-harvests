package com.pooja.farm.fresh.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.pooja.farm.fresh.entity.DiscountRule;

import java.time.DayOfWeek;
import java.util.List;

// Change from JpaRepository<Product, Long> to JpaRepository<DiscountRule, Long>
public interface DiscountRepository extends JpaRepository<DiscountRule, Long> {
    // Note: The method signature `findByDayOfWeekAndCategory(String dayOfWeek, String category)`
    // might still need adjustment, as 'category' in DiscountRule is an object, not a String.
    // You might want `findByDayOfWeekAndCategoryName(DayOfWeek dayOfWeek, String categoryName)`
    // if Category has a `name` field.
    // Consider if this repository is needed if DiscountRuleRepository already covers your needs.
    
}
