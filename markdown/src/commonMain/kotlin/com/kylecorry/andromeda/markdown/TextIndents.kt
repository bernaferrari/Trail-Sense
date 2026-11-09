package com.kylecorry.andromeda.markdown

/**
 * Whether a paragraph's [androidx.compose.ui.text.style.TextIndent] first-line indent applies again after each line
 * break inside it. Android's text layout does (a leading margin per line), so a list can be one paragraph with
 * hanging items; Skia's applies it once per paragraph, so lists there are left without a hanging indent.
 */
internal expect val indentsEachLine: Boolean
