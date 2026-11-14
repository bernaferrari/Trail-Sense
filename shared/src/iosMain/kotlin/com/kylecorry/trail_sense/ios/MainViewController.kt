package com.kylecorry.trail_sense.ios

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.delete
import com.kylecorry.trail_sense.shared.generated.resources.delete_note_title
import com.kylecorry.trail_sense.shared.generated.resources.dialog_leave
import com.kylecorry.trail_sense.shared.generated.resources.unsaved_changes
import com.kylecorry.trail_sense.shared.generated.resources.unsaved_changes_message
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.SystemBackHandler
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.text.ComposeAppStrings
import com.kylecorry.trail_sense.shared.text.IosPlatformFormats
import com.kylecorry.trail_sense.shared.text.LocalAppStrings
import com.kylecorry.trail_sense.shared.text.SystemString
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.text.systemString
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.notes.domain.Note
import com.kylecorry.trail_sense.tools.notes.infrastructure.iosNoteRepository
import com.kylecorry.trail_sense.tools.notes.ui.NoteEditorScreen
import com.kylecorry.trail_sense.tools.notes.ui.NotesScreen
import kotlinx.coroutines.launch
import platform.UIKit.UIViewController
import kotlin.time.Clock
import kotlin.time.Instant

/** Entry point for the iOS host. [databasePath] is in the app's documents directory. */
fun MainViewController(databasePath: String): UIViewController {
    val notes = iosNoteRepository(databasePath)
    val strings = ComposeAppStrings()
    val formats = IosPlatformFormats()
    return ComposeUIViewController {
        val navigator = remember { TrailSenseNavigator().apply { setStartRoute(Route.Notes) } }
        val scope = rememberCoroutineScope()
        val notesFlow = remember { notes.getNotes() }
        var deleting by remember { mutableStateOf<Note?>(null) }
        CompositionLocalProvider(LocalAppStrings provides strings, LocalNavigator provides navigator) {
            TrailSenseTheme {
                when (val route = navigator.currentRoute) {
                    is Route.NoteEditor -> {
                        var existing by remember(route.editNoteId) { mutableStateOf<Note?>(null) }
                        var hasChanges by remember(route.editNoteId) { mutableStateOf(false) }
                        var confirmLeave by remember(route.editNoteId) { mutableStateOf(false) }
                        val leave = { if (hasChanges) confirmLeave = true else navigator.navigateUp(); Unit }
                        SystemBackHandler { leave() }
                        NoteEditorScreen(
                            noteId = route.editNoteId,
                            loadNote = notes::getNote,
                            onStateChanged = { title, contents, note ->
                                existing = note
                                hasChanges = title != note?.title.orEmpty() || contents != note?.contents.orEmpty()
                            },
                            onSave = { title, contents ->
                                val note = existing?.copy(title = title, contents = contents)
                                    ?: Note(title, contents, Clock.System.now().toEpochMilliseconds())
                                notes.addNote(note)
                                navigator.navigateUp()
                            },
                            onBack = leave
                        )
                        if (confirmLeave) {
                            AlertDialog(
                                onDismissRequest = { confirmLeave = false },
                                title = { Text(appString(Res.string.unsaved_changes)) },
                                text = { Text(appString(Res.string.unsaved_changes_message)) },
                                confirmButton = {
                                    TextButton(onClick = { confirmLeave = false; navigator.navigateUp() }) {
                                        Text(appString(Res.string.dialog_leave))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { confirmLeave = false }) { Text(systemString(SystemString.Cancel)) }
                                }
                            )
                        }
                    }
                    else -> NotesScreen(
                        notes = notesFlow,
                        lifecycleOwner = LocalLifecycleOwner.current,
                        onAdd = { navigator.navigate(Route.NoteEditor()) },
                        onEdit = { navigator.navigate(Route.NoteEditor(it.id)) },
                        onDelete = { deleting = it },
                        onShowQR = null,
                        showBack = false,
                        formatNoteTimestamp = { millis ->
                            if (millis <= 0) null else formats.formatDate(
                                Instant.fromEpochMilliseconds(millis), false, false, true
                            )
                        }
                    )
                }
                deleting?.let { note ->
                    AlertDialog(
                        onDismissRequest = { deleting = null },
                        title = { Text(appString(Res.string.delete_note_title)) },
                        text = { Text(note.title?.takeIf { it.isNotBlank() } ?: systemString(SystemString.Untitled)) },
                        confirmButton = {
                            TextButton(onClick = { scope.launch { notes.deleteNote(note) }; deleting = null }) {
                                Text(appString(Res.string.delete))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { deleting = null }) { Text(systemString(SystemString.Cancel)) }
                        }
                    )
                }
            }
        }
    }
}
