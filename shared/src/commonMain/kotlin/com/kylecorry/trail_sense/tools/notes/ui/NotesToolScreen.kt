package com.kylecorry.trail_sense.tools.notes.ui


import com.kylecorry.trail_sense.shared.generated.resources.*
import kotlinx.coroutines.IO
import com.kylecorry.trail_sense.shared.text.SystemString
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.tools.notes.infrastructure.INoteRepo
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.tools.qr.ui.showQR
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.shared.navigation.rememberSystemBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.tools.qr.infrastructure.NoteQREncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** The Notes tool: every note, newest first, each with edit, delete and QR share. */
@Composable
fun NotesToolScreen(repo: INoteRepo, strings: AppStrings, formatter: FormatService) {
    val host = AppHost.current()
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val notes = remember(repo) { repo.getNotes() }
    NotesScreen(
        notes = notes,
        lifecycleOwner = LocalLifecycleOwner.current,
        formatNoteTimestamp = { millis ->
            if (millis <= 0L) null else formatter.formatDateTime(kotlin.time.Instant.fromEpochMilliseconds(millis), abbreviateMonth = true)
        },
        onAdd = { navigator.navigate(Route.NoteEditor()) },
        onEdit = { note -> navigator.navigate(Route.NoteEditor(editNoteId = note.id)) },
        onDelete = { note ->
            Alerts.dialog(host,
                strings.get(Res.string.delete_note_title),
                if (note.title?.trim().isNullOrEmpty()) {
                    strings.system(SystemString.Untitled)
                } else {
                    note.title
                }
            ) { cancelled ->
                if (!cancelled) {
                    scope.launch(Dispatchers.IO) {
                        repo.deleteNote(note)
                    }
                }
            }
        },
        onShowQR = { note ->
            showQR(
                host,
                navigator,
                note.title ?: strings.system(SystemString.Untitled),
                NoteQREncoder().encode(note)
            )
        }
    )
}
