package com.kylecorry.trail_sense.tools.notes.ui

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.promptIfUnsavedChanges
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.tools.notes.domain.Note
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

class FragmentToolNotesCreate : TrailSenseComposeFragment() {

    private val notesRepo by lazy { NoteRepo.getInstance(requireContext()) }
    private var editingNote: Note? = null
    private var noteId: Long = 0L
    private var title: String = ""
    private var content: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        noteId = arguments?.getLong("edit_note_id") ?: 0L
    }

    @Composable
    override fun FragmentContent() {
        NoteEditorScreen(
            noteId = noteId,
            loadNote = { notesRepo.getNote(it) },
            onStateChanged = { newTitle, newContent, note ->
                title = newTitle
                content = newContent
                editingNote = note
            },
            onSave = { newTitle, newContent ->
                saveNote(newTitle, newContent)
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        promptIfUnsavedChanges(::hasChanges)
    }

    private suspend fun saveNote(title: String, content: String) {
        val existingNote = editingNote
        val note = existingNote?.copy(title = title, contents = content)
            ?.apply { id = existingNote.id }
            ?: Note(title, content, Instant.now().toEpochMilli())
        withContext(Dispatchers.IO) {
            notesRepo.addNote(note)
        }
        withContext(Dispatchers.Main) {
            findNavController().navigateUp()
        }
    }

    private fun hasChanges(): Boolean {
        return !nothingEntered() && (title != editingNote?.title.orEmpty() || content != editingNote?.contents.orEmpty())
    }

    private fun nothingEntered(): Boolean {
        if (editingNote != null) {
            return false
        }

        return title.isBlank() && content.isBlank()
    }
}

@Composable
private fun NoteEditorScreen(
    noteId: Long,
    loadNote: suspend (Long) -> Note?,
    onStateChanged: (String, String, Note?) -> Unit,
    onSave: suspend (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

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

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("title_edit"),
                    label = { Text(stringResource(R.string.note_title_hint)) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 16.dp)
                        .testTag("content_edit"),
                    label = { Text(stringResource(R.string.contents)) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text
                    )
                )
            }
            FloatingActionButton(
                onClick = {
                    if (!isSaving) {
                        isSaving = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("note_create_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null
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
