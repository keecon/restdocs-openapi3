package com.keecon.restdocs.apispec

import org.springframework.restdocs.headers.HeaderDescriptor
import org.springframework.restdocs.headers.RequestHeadersSnippet
import org.springframework.restdocs.headers.ResponseHeadersSnippet
import org.springframework.restdocs.hypermedia.LinksSnippet
import org.springframework.restdocs.payload.RequestFieldsSnippet
import org.springframework.restdocs.payload.ResponseFieldsSnippet
import org.springframework.restdocs.request.FormParametersSnippet
import org.springframework.restdocs.request.ParameterDescriptor
import org.springframework.restdocs.request.PathParametersSnippet
import org.springframework.restdocs.request.QueryParametersSnippet
import org.springframework.restdocs.request.RequestPartDescriptor
import org.springframework.restdocs.request.RequestPartsSnippet
import org.springframework.restdocs.snippet.Snippet
import java.util.function.Function

abstract class RestDocumentationWrapper {

    protected fun enhanceSnippetsWithResourceSnippet(
        resourceDetails: ResourceSnippetDetails,
        snippetFilter: Function<List<Snippet>, List<Snippet>>,
        vararg snippets: Snippet
    ): Array<Snippet> {

        val enhancedSnippets =
            if (snippets.none { it is ResourceSnippet }) {
                val resourceParameters = createBuilder(resourceDetails)
                    .requestFields(
                        snippets.filterIsInstance<RequestFieldsSnippet>().flatMap {
                            DescriptorExtractor.extractDescriptorsFor(it)
                        }
                    )
                    .responseFields(
                        snippets.filterIsInstance<ResponseFieldsSnippet>().flatMap {
                            DescriptorExtractor.extractDescriptorsFor(it)
                        }
                    )
                    .links(
                        snippets.filterIsInstance<LinksSnippet>().flatMap {
                            DescriptorExtractor.extractDescriptorsFor(it)
                        }
                    )
                    .pathParameters(
                        *snippets.filterIsInstance<PathParametersSnippet>().flatMap {
                            DescriptorExtractor.extractDescriptorsFor<ParameterDescriptor>(it)
                        }.toTypedArray()
                    )
                    .queryParameters(
                        *snippets.filterIsInstance<QueryParametersSnippet>().flatMap {
                            DescriptorExtractor.extractDescriptorsFor<ParameterDescriptor>(it)
                        }.toTypedArray()
                    )
                    .formParameters(
                        *snippets.filterIsInstance<FormParametersSnippet>().flatMap {
                            DescriptorExtractor.extractDescriptorsFor<ParameterDescriptor>(it)
                        }.toTypedArray()
                    )
                    .requestParts(
                        *snippets.filterIsInstance<RequestPartsSnippet>().flatMap {
                            DescriptorExtractor.extractDescriptorsFor<RequestPartDescriptor>(it)
                        }.toTypedArray()
                    )
                    .requestHeaders(
                        *snippets.filterIsInstance<RequestHeadersSnippet>().flatMap {
                            DescriptorExtractor.extractDescriptorsFor<HeaderDescriptor>(it)
                        }.toTypedArray()
                    )
                    .responseHeaders(
                        *snippets.filterIsInstance<ResponseHeadersSnippet>().flatMap {
                            DescriptorExtractor.extractDescriptorsFor<HeaderDescriptor>(it)
                        }.toTypedArray()
                    )
                    .build()
                snippets.toList() + ResourceDocumentation.resource(resourceParameters)
            } else snippets.toList()

        return snippetFilter.apply(enhancedSnippets).toTypedArray()
    }

    private fun createBuilder(resourceDetails: ResourceSnippetDetails): ResourceSnippetParametersBuilder {
        return when (resourceDetails) {
            is ResourceSnippetParametersBuilder -> resourceDetails
            else -> ResourceSnippetParametersBuilder()
                .description(resourceDetails.description)
                .requestSchema(resourceDetails.requestSchema)
                .responseSchema(resourceDetails.responseSchema)
                .summary(resourceDetails.summary)
                .privateResource(resourceDetails.privateResource)
                .deprecated(resourceDetails.deprecated)
                .tags(*resourceDetails.tags.toTypedArray())
        }
    }
}
