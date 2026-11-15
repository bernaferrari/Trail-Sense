package com.kylecorry.trail_sense.ios

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.kylecorry.andromeda.device.DeviceSensors
import com.kylecorry.andromeda.device.LocationReading
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.delete
import com.kylecorry.trail_sense.shared.generated.resources.notes
import com.kylecorry.trail_sense.shared.generated.resources.my_location
import com.kylecorry.trail_sense.shared.generated.resources.delete_note_title
import com.kylecorry.trail_sense.shared.generated.resources.dialog_leave
import com.kylecorry.trail_sense.shared.generated.resources.unsaved_changes
import com.kylecorry.trail_sense.shared.generated.resources.unsaved_changes_message
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.SharedNavigationDisplay
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
import com.kylecorry.trail_sense.tools.location.ui.LocationContent
import com.kylecorry.trail_sense.tools.notes.domain.Note
import com.kylecorry.trail_sense.tools.notes.infrastructure.iosNoteRepository
import com.kylecorry.trail_sense.tools.notes.ui.NoteEditorScreen
import com.kylecorry.trail_sense.tools.notes.ui.NotesScreen
import kotlinx.coroutines.CancellationException
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
        var showLocation by remember { mutableStateOf(false) }
        var location by remember { mutableStateOf<LocationReading?>(null) }
        var locationUnavailable by remember { mutableStateOf(false) }
        val sensors = remember { DeviceSensors() }
        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(showLocation, lifecycleOwner) {
            if (showLocation) {
                lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    location = null
                    locationUnavailable = false
                    try {
                        sensors.location().collect { location = it.value }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        locationUnavailable = true
                    }
                }
            }
        }
        CompositionLocalProvider(LocalAppStrings provides strings, LocalNavigator provides navigator) {
            TrailSenseTheme {
                Scaffold(bottomBar = {
                    if (navigator.currentRoute !is Route.NoteEditor) {
                        NavigationBar {
                            NavigationBarItem(
                                selected = !showLocation,
                                onClick = { showLocation = false },
                                icon = { Icon(Icons.Default.Notes, contentDescription = null) },
                                label = { Text(appString(Res.string.notes)) }
                            )
                            NavigationBarItem(
                                selected = showLocation,
                                onClick = { showLocation = true },
                                icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                label = { Text(appString(Res.string.my_location)) }
                            )
                        }
                    }
                }) { padding ->
                Box(Modifier.padding(padding)) {
                SharedNavigationDisplay(navigator) { route ->
                when (route) {
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
                    else -> if (showLocation) {
                        LocationContent(location, locationUnavailable)
                    } else NotesScreen(
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
                }
                }
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
