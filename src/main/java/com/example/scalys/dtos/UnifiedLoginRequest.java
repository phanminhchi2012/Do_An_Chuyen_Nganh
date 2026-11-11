package com.example.scalys.dtos;

import jakarta.validation.constraints.NotBlank;

public record UnifiedLoginRequest(@NotBlank String identifier, @NotBlank String password) { }
