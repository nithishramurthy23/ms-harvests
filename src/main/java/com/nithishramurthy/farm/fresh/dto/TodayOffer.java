package com.nithishramurthy.farm.fresh.dto;

import java.math.BigDecimal;

/** Today's category discount, for example 10% off "fruits". */
public record TodayOffer(String category, BigDecimal percent) {
}
