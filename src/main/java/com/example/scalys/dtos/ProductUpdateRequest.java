package com.example.scalys.dtos;

import com.example.scalys.models.ProductKind;
import com.example.scalys.models.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ProductUpdateRequest(
        @Valid @NotNull ProductRequest product,
        @NotNull ProductKind expectedKind,
        @NotNull ProductStatus expectedStatus,
        Integer expectedStockQuantity
) { }
