package com.kylecorry.andromeda.clipboard

import android.content.Context

fun clipboardWriter(context: Context): ClipboardWriter = ClipboardWriter { text ->
    Clipboard.copy(context, text, null)
}
