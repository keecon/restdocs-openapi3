package com.keecon.restdocs.apispec.example.dto;

import com.keecon.restdocs.apispec.example.model.FileType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductResultUpdateRequest(@NotNull @Min(1) Long productId,
										 @NotNull FileType fileType,
										 @NotEmpty @Size(min = 10, max = 100) String fileUrl) {
}
