package com.keecon.restdocs.apispec.example;

import com.keecon.restdocs.apispec.example.model.FileType;
import com.keecon.restdocs.apispec.example.model.ProductResult;
import com.keecon.restdocs.apispec.example.model.ProductResultAssignObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

	public ProductResult getResult(Long productId, Long code, Integer seq) {
		// example data set
		return ProductResult.builder()
			.code(code)
			.seq(seq)
			.score(BigDecimal.TEN)
			.assign(ProductResultAssignObject.builder()
				.code(1L)
				.seq(1)
				.objectId(1L)
				.fileType(FileType.VIDEO)
				.fileUrl("/assets/product/1/1/1/XMHwNo6DDTYfh3wWfC7zsG.mp4")
				.comment("bad quality")
				.build()
			)
			.assign(ProductResultAssignObject.builder()
				.code(1L)
				.seq(2)
				.objectId(2L)
				.fileType(FileType.SOUND)
				.fileUrl("/assets/product/1/2/2/IyzfoVnVlKeHfZRIyR6pVP.mp3")
				.build()
			)
			.build();
	}
}
