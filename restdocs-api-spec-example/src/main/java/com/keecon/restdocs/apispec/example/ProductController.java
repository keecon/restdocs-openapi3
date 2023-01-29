package com.keecon.restdocs.apispec.example;

import com.keecon.restdocs.apispec.example.dto.*;
import com.keecon.restdocs.apispec.example.model.ProductResultAssignObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(
	path = "/v1/products",
	produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;

	@PostMapping("/{productId}/result")
	public ProductResultCreateResponse createResult(@Valid ProductResultCreateRequest req,
													@Valid @RequestBody ProductResultCreateRequestBody reqBody) {
		val resultCode = productService.createResult(req.productId(), reqBody.result());
		return new ProductResultCreateResponse("OK", resultCode);
	}

	@GetMapping("/{productId}/result")
	public ProductResultResponse getResult(@Valid ProductResultRequest req) {
		val result = productService.getResult(req.productId(), req.code(), req.seqOrDefault());
		return new ProductResultResponse(result);
	}

	@PutMapping("/{productId}/result/files")
	public ProductResultUpdateResponse updateResult(@Valid ProductResultUpdateRequest req) {
		val object = ProductResultAssignObject.builder()
			.code(3L)
			.seq(3)
			.objectId(3L)
			.fileType(req.fileType())
			.fileUrl(req.fileUrl())
			.build();
		val rowAffected = productService.updateResult(req.productId(), object);
		return new ProductResultUpdateResponse("OK");
	}
}
