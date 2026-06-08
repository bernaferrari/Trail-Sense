@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.notes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.notes.domain.Note

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    notes: LiveData<List<Note>>,
    lifecycleOwner: LifecycleOwner,
    onAdd: () -> Unit,
    onEdit: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onShowQR: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    var items by remember { mutableStateOf(emptyList<Note>()) }

    DisposableEffect(notes, lifecycleOwner) {
        val observer = Observer<List<Note>> {
            items = it.orEmpty().sortedByDescending { note -> note.createdOn }
        }
        notes.observe(lifecycleOwner, observer)
        onDispose {
            notes.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.tool_notes_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                modifier = Modifier.testTag("add_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add)
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (items.isEmpty()) {
                Text(
                    text = stringResource(R.string.notes_empty_text),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("notes_empty_text"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("note_list"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)
                ) {
                    items(items, key = { it.id }) { note ->
                        NoteListItem(
                            note = note,
                            onEdit = onEdit,
                            onDelete = onDelete,
                            onShowQR = onShowQR
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteListItem(
    note: Note,
    onEdit: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onShowQR: (Note) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    val title = if (note.title?.trim().isNullOrEmpty()) {
        stringResource(android.R.string.untitled)
    } else {
        note.title.orEmpty()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(note) }
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = note.contents.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box {
            IconButton(onClick = { isMenuOpen = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null
                )
            }
            DropdownMenu(
                expanded = isMenuOpen,
                onDismissRequest = { isMenuOpen = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.qr_code)) },
                    onClick = {
                        isMenuOpen = false
                        onShowQR(note)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete)) },
                    onClick = {
                        isMenuOpen = false
                        onDelete(note)
                    }
                )
            }
        }
    }
}
