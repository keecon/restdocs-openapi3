package com.keecon.restdocs.apispec

import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.PayloadDocumentation.beneathPath
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.partWithName
import org.springframework.restdocs.request.RequestDocumentation.requestParts
import org.springframework.restdocs.request.RequestPartDescriptor

class DescriptorExtractorTest {

    @Test
    fun should_extract_request_part_descriptors() {
        val descriptor = partWithName("file").description("uploaded file")

        val extracted = DescriptorExtractor.extractDescriptorsFor<RequestPartDescriptor>(
            requestParts(descriptor)
        )

        then(extracted).containsExactly(descriptor)
    }

    @Test
    fun should_prefix_field_descriptors_with_beneath_path() {
        val extracted = DescriptorExtractor.extractDescriptorsFor<FieldDescriptor>(
            responseFields(
                beneathPath("_links"),
                fieldWithPath("self.href").description("self link")
            )
        )

        then(extracted.map { it.path }).containsExactly("_links.self.href")
    }
}
