package com.nithishramurthy.farm.fresh.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.nithishramurthy.farm.fresh.entity.DiscountRule;

import java.time.DayOfWeek;
import java.util.List;

/** Older duplicate of DiscountRuleRepository. Not used; kept until it can be removed safely. */
public interface DiscountRepository extends JpaRepository<DiscountRule, Long> {
    
}
