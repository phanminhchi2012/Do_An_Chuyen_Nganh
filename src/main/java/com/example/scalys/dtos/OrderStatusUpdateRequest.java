package com.example.scalys.dtos;

import com.example.scalys.models.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(@NotNull OrderStatus status) {
}
