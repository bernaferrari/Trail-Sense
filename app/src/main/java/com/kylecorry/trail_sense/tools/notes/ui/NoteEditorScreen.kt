@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.notes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.notes.domain.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun NoteEditorScreen(
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(TrailSenseSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("title_edit"),
                label = { Text(stringResource(R.string.note_title_hint)) },
                textStyle = MaterialTheme.typography.titleMedium,
                shape = MaterialTheme.shapes.large,
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
                    .testTag("content_edit"),
                label = { Text(stringResource(R.string.contents)) },
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    keyboardType = KeyboardType.Text
                )
            )
            TrailSenseButton(
                text = stringResource(R.string.save),
                onClick = {
                    if (!isSaving) {
                        isSaving = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_create_btn")
            )
        }
    }

    LaunchedEffect(isSaving) {
        if (isSaving) {
            onSave(title, content)
        }
    }
}
