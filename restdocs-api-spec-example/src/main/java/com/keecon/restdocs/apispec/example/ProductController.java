package com.keecon.restdocs.apispec.example;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(
	path = "/v1/products",
	produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;

	/**
	 * product result detail info
	 *
	 * @return result info
	 */
	@GetMapping("/{productId}/result")
	public ProductResultResponse getResult(@Valid ProductResultRequest req) {
		val result = productService.getResult(req.productId(), req.code(), req.seqOrDefault());
		return new ProductResultResponse(result);
	}
}
