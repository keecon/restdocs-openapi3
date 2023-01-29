package com.keecon.restdocs.apispec.example.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductResultCreateResponse(@NotNull String status,
										  @Min(1) Long code) {
}
