package com.example.scalys.dtos;

import com.example.scalys.models.ProductKind;
import com.example.scalys.models.ProductStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductStockRequest(
        @NotNull ProductKind expectedKind,
        @NotNull ProductStatus expectedStatus,
        @NotNull @Min(0) Integer expectedStockQuantity,
        @NotNull @Min(0) Integer stockQuantity
) { }
