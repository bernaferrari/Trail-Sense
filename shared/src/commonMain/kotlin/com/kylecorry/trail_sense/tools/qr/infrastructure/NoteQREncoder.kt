package com.kylecorry.trail_sense.tools.qr.infrastructure

import kotlin.time.Clock

import com.kylecorry.trail_sense.tools.notes.domain.Note

class NoteQREncoder : IQREncoder<Note> {

    override fun encode(value: Note): String {
        val title = if (value.title != null) {
            "${value.title}\n\n\n"
        } else {
            ""
        }

        val contents = value.contents ?: ""

        return "$title$contents"
    }

    /** Older untitled notes use plain text. They can be imported through the explicit Save as note action. */
    fun decodeTitledNote(qr: String): Note? {
        val split = qr.split("\n\n\n", limit = 2)
        if (split.size != 2 || split[0].contains('\n')) return null
        return Note(split[0], split[1], Clock.System.now().toEpochMilliseconds())
    }

    override fun decode(qr: String): Note {
        return decodeTitledNote(qr) ?: Note(null, qr, Clock.System.now().toEpochMilliseconds())
    }
}
