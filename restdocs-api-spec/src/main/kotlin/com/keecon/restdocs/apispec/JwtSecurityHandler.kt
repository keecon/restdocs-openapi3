package com.keecon.restdocs.apispec

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.http.HttpHeaders
import org.springframework.restdocs.operation.Operation
import java.io.IOException
import java.util.Base64
import java.util.Collections.emptyList

/**
 * Extract a list of scopes from a JWT token
 */
internal class JwtSecurityHandler : SecurityRequirementsExtractor {

    override fun extractSecurityRequirements(operation: Operation): SecurityRequirements? {
        if (!hasJWTBearer(operation)) return null

        val scopes = extractScopes(operation)
        return if (scopes.isNotEmpty()) Oauth2(scopes) else JWTBearer
    }

    private fun hasJWTBearer(operation: Operation): Boolean {
        return getJWT(operation).any { isJWT(it) }
    }

    private fun getJWT(operation: Operation) = operation.request.headers
        .filterKeys { it == HttpHeaders.AUTHORIZATION }
        .flatMap { it.value }
        .filter { it.startsWith("Bearer ") }
        .map { it.replace("Bearer ", "") }

    private fun isJWT(jwt: String): Boolean {
        val jwtParts = jwt.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
        if (jwtParts.size >= 2) { // JWT = header, payload, signature; at least the first two should be there
            val decodedJwtHeader = decodeJwtPart(jwtParts[0]) ?: return false
            try {
                return ObjectMapper().readValue<Map<String, Any>?>(decodedJwtHeader)?.containsKey("alg") == true
            } catch (e: IOException) {
                // probably not JWT
            }
        }
        return false
    }

    private fun extractScopes(operation: Operation): List<String> {
        return getJWT(operation).flatMap { jwt2scopes(it) }
    }

    private fun jwt2scopes(jwt: String): List<String> {
        val jwtParts = jwt.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
        if (jwtParts.size >= 2) { // JWT = header, payload, signature; at least the first two should be there
            val decodedPayload = decodeJwtPart(jwtParts[1]) ?: return emptyList()
            try {
                val jwtMap = ObjectMapper().readValue<Map<String, Any>?>(decodedPayload) ?: return emptyList()
                val scope = jwtMap["scope"]
                return when (scope) {
                    is List<*> -> scope.filterIsInstance<String>().filter(String::isNotBlank)
                    is String -> scope.trim().split("\\s+".toRegex()).filter(String::isNotBlank)
                    else -> emptyList()
                }
            } catch (e: IOException) {
                // probably not JWT
            }
        }

        return emptyList()
    }

    private fun decodeJwtPart(value: String): String? = try {
        String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
    } catch (@Suppress("SwallowedException") exception: IllegalArgumentException) {
        null
    }
}
