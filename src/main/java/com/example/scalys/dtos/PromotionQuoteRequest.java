package com.example.scalys.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PromotionQuoteRequest(
        @NotNull @Size(min = 1, max = 20) List<@Valid Item> items,
        @Size(max = 5) List<@Size(min = 3, max = 32) String> promotionCodes) {
    public record Item(@NotNull @Positive Long productId, @NotNull @Min(1) Integer quantity) {}
}
