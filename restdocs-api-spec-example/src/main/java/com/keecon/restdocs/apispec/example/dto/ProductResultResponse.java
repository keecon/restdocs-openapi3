package com.keecon.restdocs.apispec.example.dto;

import com.keecon.restdocs.apispec.example.model.ProductResult;
import jakarta.validation.constraints.NotNull;

public record ProductResultResponse(@NotNull ProductResult result) {
}
