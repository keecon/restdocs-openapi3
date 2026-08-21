package com.keecon.restdocs.apispec

import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
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
}
