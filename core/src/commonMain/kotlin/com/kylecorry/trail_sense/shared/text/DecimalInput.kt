package com.kylecorry.trail_sense.shared.text

/**
 * Reads a number the user typed. Either decimal separator is accepted, so "2,5" and "2.5" are both 2.5
 * whatever the device locale is. Null while the text isn't a finite number yet (empty, "-", ",").
 */
fun String.toDecimalInputOrNull(): Float? {
    return trim().replace(',', '.').toFloatOrNull()?.takeIf { it.isFinite() }
}
