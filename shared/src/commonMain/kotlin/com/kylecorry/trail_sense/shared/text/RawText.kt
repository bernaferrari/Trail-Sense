package com.kylecorry.trail_sense.shared.text

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/**
 * A text file bundled with the app, named by its file name without the extension (`guide_tool_compass`).
 *
 * The files live in the app's `res/raw` folders, where they are written and translated; [RawTexts] lists them so a
 * misspelt name doesn't compile, and a [RawTextReader] reads one in the user's language.
 */
@Serializable
@JvmInline
value class RawText(val name: String)

/** Reads the bundled text files. */
interface RawTextReader {
    /** The text of [text] in the user's language, falling back to the untranslated file. */
    suspend fun read(text: RawText): String
}
