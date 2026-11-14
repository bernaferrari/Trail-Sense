package com.kylecorry.andromeda.clipboard

import android.content.Context

public fun clipboardWriter(context: Context): ClipboardWriter = ClipboardWriter { text ->
    Clipboard.copy(context, text)
}
