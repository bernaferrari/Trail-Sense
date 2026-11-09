package com.kylecorry.trail_sense.shared.andromeda_temp.markdown

import android.text.Spannable

class MarkdownExtension(
    val length: Int,
    val openingCharacter: Char,
    val closingCharacter: Char = openingCharacter,
    val spanProducer: () -> Any
)