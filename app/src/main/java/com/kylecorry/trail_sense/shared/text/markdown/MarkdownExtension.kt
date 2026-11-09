package com.kylecorry.trail_sense.shared.text.markdown

import android.text.Spannable

class MarkdownExtension(
    val length: Int,
    val openingCharacter: Char,
    val closingCharacter: Char = openingCharacter,
    val spanProducer: () -> Any
)