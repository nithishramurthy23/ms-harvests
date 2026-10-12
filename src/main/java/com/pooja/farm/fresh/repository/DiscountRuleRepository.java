package com.pooja.farm.fresh.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pooja.farm.fresh.entity.DiscountRule;

import java.time.DayOfWeek;
import java.util.List;

public interface DiscountRuleRepository extends JpaRepository<DiscountRule, Long> {
    List<DiscountRule> findByDayOfWeekAndActive(DayOfWeek dayOfWeek, boolean active);
}
