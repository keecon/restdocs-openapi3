package com.keecon.restdocs.apispec.example.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductResultCreateRequest(@NotNull @Min(1) Long productId) {
}
