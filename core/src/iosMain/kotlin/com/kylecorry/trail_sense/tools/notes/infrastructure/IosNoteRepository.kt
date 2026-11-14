package com.kylecorry.trail_sense.tools.notes.infrastructure

import com.kylecorry.trail_sense.main.persistence.iosAppDatabase

/** Opens the shared database and supplies its notes repository. */
fun iosNoteRepository(path: String): INoteRepo = NoteRepo(iosAppDatabase(path).noteDao())
