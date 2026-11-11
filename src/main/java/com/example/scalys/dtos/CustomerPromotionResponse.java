package com.example.scalys.dtos;

import com.example.scalys.models.CustomerPromotionCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CustomerPromotionResponse(String code, String name, BigDecimal discountPercent,
                                        BigDecimal minimumTotal, boolean newCustomerOnly,
                                        Instant startsAt, Instant endsAt, String status,
                                        Instant usedAt, List<String> usedOrderCodes) {
    public static CustomerPromotionResponse from(CustomerPromotionCode code, String status,
                                                 List<String> usedOrderCodes) {
        var promotion = code.getPromotion();
        return new CustomerPromotionResponse(code.getCode(), promotion.getName(),
                promotion.getDiscountPercent(), promotion.getMinimumTotal(),
                promotion.isNewCustomerOnly(), promotion.getStartsAt(), promotion.getEndsAt(), status,
                code.getUsedAt(), usedOrderCodes);
    }
}
