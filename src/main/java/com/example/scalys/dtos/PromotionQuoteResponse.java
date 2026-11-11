package com.example.scalys.dtos;

import java.math.BigDecimal;

public record PromotionQuoteResponse(BigDecimal subtotal, BigDecimal discountAmount,
                                     BigDecimal total, String appliedCode,
                                     BigDecimal discountPercent) {}
