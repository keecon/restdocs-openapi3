package com.keecon.restdocs.apispec.example.model;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class ProductResult {

	@NotNull
	@Min(1)
	Long code;

	@NotNull
	@Min(1)
	Integer seq;

	@NotNull
	@DecimalMin("0.0")
	@DecimalMax("100.0")
	@Digits(integer = 3, fraction = 2)
	BigDecimal score;

	@NotEmpty
	@Singular
	List<@NotNull ProductResultAssignObject> assigns;
}
