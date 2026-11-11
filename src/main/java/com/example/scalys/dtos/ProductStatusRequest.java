package com.example.scalys.dtos;

import com.example.scalys.models.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record ProductStatusRequest(@NotNull ProductStatus status) {
}
