package com.keecon.restdocs.apispec.jsonschema

import org.everit.json.schema.FormatValidator
import java.time.DateTimeException
import java.time.LocalDate
import java.util.Optional

internal class Rfc3339DateTimeFormatValidator : FormatValidator {

    override fun validate(subject: String?): Optional<String> {
        if (subject == null) return Optional.empty()
        val match = WIRE_SHAPE.matchEntire(subject) ?: return invalid(subject)

        return if (isValid(match)) Optional.empty() else invalid(subject)
    }

    override fun formatName() = "date-time"

    private fun invalid(subject: String) = Optional.of(
        "'$subject' does not match the supported RFC 3339 date-time profile " +
            "(expected yyyy-MM-dd[Tt]HH:mm:ss[.fraction](Z|z|+/-HH:mm), seconds 00-59)"
    )

    private fun isValid(match: MatchResult): Boolean {
        val groups = match.groupValues
        try {
            LocalDate.of(groups[YEAR].toInt(), groups[MONTH].toInt(), groups[DAY].toInt())
        } catch (@Suppress("SwallowedException") exception: DateTimeException) {
            return false
        }
        val hour = groups[HOUR].toInt()
        val minute = groups[MINUTE].toInt()
        val second = groups[SECOND].toInt()
        val offsetHour = groups[OFFSET_HOUR].ifEmpty { "0" }.toInt()
        val offsetMinute = groups[OFFSET_MINUTE].ifEmpty { "0" }.toInt()

        if (hour !in 0..23 || minute !in 0..59) return false
        if (offsetHour !in 0..23 || offsetMinute !in 0..59) return false
        return second in 0..59
    }

    private companion object {
        val WIRE_SHAPE = Regex(
            """^(\d{4})-(\d{2})-(\d{2})[Tt](\d{2}):(\d{2}):(\d{2})(?:\.(\d+))?([Zz]|([+-])(\d{2}):(\d{2}))$"""
        )

        const val YEAR = 1
        const val MONTH = 2
        const val DAY = 3
        const val HOUR = 4
        const val MINUTE = 5
        const val SECOND = 6
        const val OFFSET_HOUR = 10
        const val OFFSET_MINUTE = 11
    }
}
