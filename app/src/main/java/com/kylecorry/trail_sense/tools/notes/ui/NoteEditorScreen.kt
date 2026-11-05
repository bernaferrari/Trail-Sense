@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.notes.ui

import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.views.compose.UnsavedChangesBackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
import com.kylecorry.trail_sense.tools.notes.domain.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteRepo
import java.time.Instant

/**
 * The note editor: the title and the contents as one grouped object, the contents roomy enough to write in, and Save
 * in the top bar so it is always one tap away however long the note gets.
 */
@Composable
fun NoteEditorScreen(
    noteId: Long,
    loadNote: suspend (Long) -> Note?,
    onStateChanged: (String, String, Note?) -> Unit,
    onSave: suspend (String, String) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val back: () -> Unit = onBack ?: { dispatcher?.onBackPressed() }

    LaunchedEffect(noteId) {
        if (noteId != 0L) {
            editingNote = withContext(Dispatchers.IO) {
                loadNote(noteId)
            }
            title = editingNote?.title.orEmpty()
            content = editingNote?.contents.orEmpty()
            onStateChanged(title, content, editingNote)
        }
    }

    LaunchedEffect(title, content, editingNote) {
        onStateChanged(title, content, editingNote)
    }

    TrailSensePage(
        title = stringResource(R.string.note),
        onBack = back,
        backLabel = stringResource(R.string.back),
        modifier = modifier,
        actions = {
            Button(
                onClick = {
                    if (!isSaving) {
                        isSaving = true
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .padding(end = TrailSenseSpacing.sm)
                    .testTag("note_create_btn")
            ) {
                Text(stringResource(R.string.save))
            }
        }
    ) {
        item(key = "fields") {
            Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField(
                    value = title,
                    onValueChange = { title = it },
                    label = stringResource(R.string.note_title_hint),
                    position = TrailSenseGroupPosition(isFirst = true, isLast = false),
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                    modifier = Modifier.testTag("title_edit")
                )
                com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField(
                    value = content,
                    onValueChange = { content = it },
                    label = stringResource(R.string.contents),
                    position = TrailSenseGroupPosition(isFirst = false, isLast = true),
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                    singleLine = false,
                    minLines = 12,
                    modifier = Modifier.testTag("content_edit")
                )
            }
        }
    }

    LaunchedEffect(isSaving) {
        if (isSaving) {
            onSave(title, content)
        }
    }
}

/** The note editor for [Route.NoteEditor]; back asks before dropping unsaved changes. */
@Composable
fun NoteEditorToolScreen(route: Route.NoteEditor) {
    val context = LocalContext.current.applicationContext
    val navigator = LocalNavigator.current
    val viewModel = viewModel { NoteEditorViewModel(NoteRepo.getInstance(context)) }
    UnsavedChangesBackHandler(hasChanges = viewModel.hasChanges)
    LaunchedEffect(viewModel) {
        viewModel.saved.collect { navigator.navigateUp() }
    }
    NoteEditorScreen(
        noteId = route.editNoteId,
        loadNote = viewModel::loadNote,
        onStateChanged = viewModel::onStateChanged,
        onSave = viewModel::save
    )
}

/** What the editor has typed against the note being edited; reports [saved] once the note is stored. */
internal class NoteEditorViewModel(private val repo: NoteRepo) : ViewModel() {
    private var editingNote by mutableStateOf<Note?>(null)
    private var title by mutableStateOf("")
    private var content by mutableStateOf("")

    private val _saved = Channel<Unit>(Channel.CONFLATED)
    /** One event per save, after the note is stored. */
    val saved: Flow<Unit> = _saved.receiveAsFlow()

    suspend fun loadNote(id: Long): Note? {
        return repo.getNote(id)
    }

    fun onStateChanged(title: String, content: String, note: Note?) {
        this.title = title
        this.content = content
        editingNote = note
    }

    suspend fun save(title: String, content: String) {
        val existingNote = editingNote
        val note = existingNote?.copy(title = title, contents = content)
            ?.apply { id = existingNote.id }
            ?: Note(title, content, Instant.now().toEpochMilli())
        withContext(Dispatchers.IO) {
            repo.addNote(note)
        }
        _saved.send(Unit)
    }

    val hasChanges: Boolean
        get() = !nothingEntered() &&
                (title != editingNote?.title.orEmpty() ||
                        content != editingNote?.contents.orEmpty())

    private fun nothingEntered(): Boolean {
        if (editingNote != null) {
            return false
        }

        return title.isBlank() && content.isBlank()
    }
}
