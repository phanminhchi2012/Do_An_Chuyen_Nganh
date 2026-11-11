package com.example.scalys.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record PromotionRequest(
        @NotBlank @Size(max = 16) String codePrefix,
        @NotBlank @Size(max = 150) String name,
        @NotNull BigDecimal discountPercent,
        @NotNull BigDecimal minimumTotal,
        boolean newCustomerOnly,
        boolean enabled,
        @NotNull Instant startsAt,
        Instant endsAt) {}
