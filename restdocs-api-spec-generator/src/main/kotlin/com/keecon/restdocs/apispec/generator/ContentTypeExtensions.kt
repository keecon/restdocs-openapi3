package com.keecon.restdocs.apispec.generator

internal fun String?.isJsonContentType(): Boolean {
    val subtype = this
        ?.substringBefore(';')
        ?.trim()
        ?.substringAfter('/', missingDelimiterValue = "")
        ?.trim()
        ?: return false
    return subtype.equals("json", ignoreCase = true) || subtype.endsWith("+json", ignoreCase = true)
}
