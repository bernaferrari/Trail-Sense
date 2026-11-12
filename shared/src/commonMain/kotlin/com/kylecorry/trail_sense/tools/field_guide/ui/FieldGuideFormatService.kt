package com.kylecorry.trail_sense.tools.field_guide.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideNameDisplay

class FieldGuideFormatService(private val preferences: UserPreferences) {

    fun formatTitle(page: FieldGuidePage): AnnotatedString {
        return if (preferences.fieldGuide.nameDisplay == FieldGuideNameDisplay.Scientific) {
            page.scientificName?.takeIf { it.isNotBlank() }?.let(::italicScientific) ?: AnnotatedString(page.name)
        } else {
            AnnotatedString(page.name)
        }
    }

    fun formatSubtitle(page: FieldGuidePage): AnnotatedString? {
        val scientificName = page.scientificName?.takeIf { it.isNotBlank() } ?: return null
        return if (preferences.fieldGuide.nameDisplay == FieldGuideNameDisplay.Scientific) {
            AnnotatedString(page.name)
        } else {
            italicScientific(scientificName)
        }
    }

    fun formatName(page: FieldGuidePage): String {
        val scientificName = page.scientificName?.takeIf { it.isNotBlank() }
        return when (preferences.fieldGuide.nameDisplay) {
            FieldGuideNameDisplay.Scientific -> scientificName ?: page.name
            FieldGuideNameDisplay.Both -> if (scientificName == null) {
                page.name
            } else {
                "${page.name} ($scientificName)"
            }
            else -> page.name
        }
    }

    fun formatNameAnnotated(page: FieldGuidePage): AnnotatedString {
        val scientificName = page.scientificName?.takeIf { it.isNotBlank() } ?: return AnnotatedString(page.name)
        return when (preferences.fieldGuide.nameDisplay) {
            FieldGuideNameDisplay.Scientific -> italicScientific(scientificName)
            FieldGuideNameDisplay.Both -> buildAnnotatedString {
                append(page.name)
                append(" (")
                append(italicScientific(scientificName))
                append(")")
            }
            else -> AnnotatedString(page.name)
        }
    }

    private fun italicScientific(scientificName: String): AnnotatedString {
        val suffixStart = scientificName.indexOfFirst { it.isWhitespace() }
        val italicEnd = if (
            suffixStart >= 0 && scientificName.substring(suffixStart).trim() in setOf("sp.", "spp.")
        ) {
            suffixStart
        } else {
            scientificName.length
        }
        return buildAnnotatedString {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(scientificName.substring(0, italicEnd))
            }
            if (italicEnd < scientificName.length) {
                append(scientificName.substring(italicEnd))
            }
        }
    }
}
