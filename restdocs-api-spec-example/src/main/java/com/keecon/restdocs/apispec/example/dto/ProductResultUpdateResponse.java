package com.keecon.restdocs.apispec.example.dto;

import jakarta.validation.constraints.NotNull;

public record ProductResultUpdateResponse(@NotNull String status) {
}
