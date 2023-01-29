package com.keecon.restdocs.apispec.example.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductResultRequest(@NotNull @Min(1) Long productId,
								   @NotNull @Min(1) Long code,
								   @Min(1) Integer seq) {
	public static final Integer DEFAULT_RESULT_SEQ = 1;

	public Integer seqOrDefault() {
		return seq != null ? seq : DEFAULT_RESULT_SEQ;
	}
}
