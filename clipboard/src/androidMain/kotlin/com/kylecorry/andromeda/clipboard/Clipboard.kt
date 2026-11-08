package com.kylecorry.andromeda.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.getSystemService

object Clipboard {

    fun copy(context: Context, text: String) {
        val clipboardManager = context.getSystemService<ClipboardManager>()
        clipboardManager?.setPrimaryClip(ClipData.newPlainText(text, text))
    }

}