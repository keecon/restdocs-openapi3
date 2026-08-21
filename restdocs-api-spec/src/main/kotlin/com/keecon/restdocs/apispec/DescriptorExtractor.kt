package com.keecon.restdocs.apispec

import org.springframework.restdocs.headers.AbstractHeadersSnippet
import org.springframework.restdocs.headers.HeaderDescriptor
import org.springframework.restdocs.hypermedia.LinkDescriptor
import org.springframework.restdocs.hypermedia.LinksSnippet
import org.springframework.restdocs.payload.AbstractFieldsSnippet
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.request.AbstractParametersSnippet
import org.springframework.restdocs.request.ParameterDescriptor
import org.springframework.restdocs.request.RequestPartDescriptor
import org.springframework.restdocs.request.RequestPartsSnippet
import org.springframework.restdocs.snippet.AbstractDescriptor
import org.springframework.restdocs.snippet.Snippet
import java.util.Collections.emptyList

@Suppress("UNCHECKED_CAST")
internal object DescriptorExtractor {

    fun <T : AbstractDescriptor<T>> extractDescriptorsFor(snippet: Snippet): List<T> {
        return when (snippet) {
            is AbstractFieldsSnippet -> extractFields(snippet) as List<T>
            is LinksSnippet -> extractLinks(snippet) as List<T>
            is AbstractHeadersSnippet -> extractHeaders(snippet) as List<T>
            is AbstractParametersSnippet -> extractParameters(snippet) as List<T>
            is RequestPartsSnippet -> extractParts(snippet) as List<T>
            else -> emptyList()
        }
    }

    private fun extractFields(snippet: AbstractFieldsSnippet): List<FieldDescriptor> =
        invokeDescriptorAccessor(snippet, AbstractFieldsSnippet::class.java, "getFieldDescriptors")

    private fun extractLinks(snippet: LinksSnippet): List<LinkDescriptor> =
        invokeDescriptorAccessor<Map<String, LinkDescriptor>>(
            snippet,
            LinksSnippet::class.java,
            "getDescriptorsByRel"
        ).values.toList()

    private fun extractHeaders(snippet: AbstractHeadersSnippet): List<HeaderDescriptor> =
        invokeDescriptorAccessor(snippet, AbstractHeadersSnippet::class.java, "getHeaderDescriptors")

    private fun extractParameters(snippet: AbstractParametersSnippet): List<ParameterDescriptor> =
        invokeDescriptorAccessor<Map<String, ParameterDescriptor>>(
            snippet,
            AbstractParametersSnippet::class.java,
            "getParameterDescriptors"
        ).values.toList()

    private fun extractParts(snippet: RequestPartsSnippet): List<RequestPartDescriptor> =
        readDescriptorField<Map<String, RequestPartDescriptor>>(
            snippet,
            RequestPartsSnippet::class.java,
            "descriptorsByName"
        ).values.toList()

    private fun <T> invokeDescriptorAccessor(target: Any, owner: Class<*>, name: String): T {
        try {
            val method = owner.getDeclaredMethod(name).apply { trySetAccessible() }
            return method.invoke(target) as T
        } catch (exception: ReflectiveOperationException) {
            throw IllegalStateException(
                "Spring REST Docs descriptor API changed: ${owner.name}#$name",
                exception
            )
        }
    }

    private fun <T> readDescriptorField(target: Any, owner: Class<*>, name: String): T {
        try {
            val field = owner.getDeclaredField(name).apply { trySetAccessible() }
            return field.get(target) as T
        } catch (exception: ReflectiveOperationException) {
            throw IllegalStateException(
                "Spring REST Docs descriptor API changed: ${owner.name}#$name",
                exception
            )
        }
    }
}
