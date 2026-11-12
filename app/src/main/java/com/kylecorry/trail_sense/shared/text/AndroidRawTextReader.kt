package com.kylecorry.trail_sense.shared.text

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.luna.concurrency.onIO

/** Reads [RawText]s from the app's raw resources, which Android resolves to the user's language. */
class AndroidRawTextReader(private val context: Context) : RawTextReader {

    override suspend fun read(text: RawText): String = onIO {
        context.readRawText(resourceId(text))
    }

    // RawTexts is generated from the raw folder, so every name resolves; keep.xml keeps the files through shrinking.
    @SuppressLint("DiscouragedApi")
    private fun resourceId(text: RawText): Int {
        val id = context.resources.getIdentifier(text.name, "raw", context.packageName)
        require(id != 0) { "No raw resource named ${text.name}" }
        return id
    }
}
