package com.kylecorry.trail_sense.shared.text

import android.content.Context
import androidx.annotation.RawRes

/** Reads a raw resource bundled with the app as UTF-8 text. */
fun Context.readRawText(@RawRes resource: Int): String {
    return resources.openRawResource(resource).use {
        it.bufferedReader().readText()
    }
}
