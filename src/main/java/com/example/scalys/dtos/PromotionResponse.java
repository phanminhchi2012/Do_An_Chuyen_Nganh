package com.example.scalys.dtos;

import com.example.scalys.models.Promotion;
import java.math.BigDecimal;
import java.time.Instant;

public record PromotionResponse(Long id, String codePrefix, String name,
                                BigDecimal discountPercent, BigDecimal minimumTotal,
                                boolean newCustomerOnly, boolean enabled,
                                Instant startsAt, Instant endsAt) {
    public static PromotionResponse from(Promotion promotion) {
        return new PromotionResponse(promotion.getId(), promotion.getCodePrefix(),
                promotion.getName(), promotion.getDiscountPercent(), promotion.getMinimumTotal(),
                promotion.isNewCustomerOnly(), promotion.isEnabled(),
                promotion.getStartsAt(), promotion.getEndsAt());
    }
}
