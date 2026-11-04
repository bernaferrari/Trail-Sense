@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.notes.ui

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

