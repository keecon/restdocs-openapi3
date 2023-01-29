package com.keecon.restdocs.apispec.example.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ProductResultAssignObject {

	@NotNull
	@Min(1)
	Long code;

	@NotNull
	@Min(1)
	Integer seq;

	@NotNull
	@Min(1)
	Long objectId;

	FileType fileType;

	String fileUrl;

	@Singular
	List<@NotEmpty String> comments;

}
