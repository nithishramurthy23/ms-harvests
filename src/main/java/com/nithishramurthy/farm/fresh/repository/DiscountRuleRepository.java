package com.nithishramurthy.farm.fresh.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nithishramurthy.farm.fresh.entity.DiscountRule;

import java.time.DayOfWeek;
import java.util.List;

/** Database access for weekday discount rules. */
public interface DiscountRuleRepository extends JpaRepository<DiscountRule, Long> {
    List<DiscountRule> findByDayOfWeekAndActive(DayOfWeek dayOfWeek, boolean active);
}
