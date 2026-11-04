@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.notes.ui

import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
import com.kylecorry.trail_sense.tools.notes.domain.Note
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The notes list: newest first, one grouped card of rows (title, then the date and the start of the note), with the
 * note's QR code and delete in each row's overflow. Nothing is shown until the notes have loaded, so "No notes"
 * never flashes on a list that has some.
 */
@Composable
fun NotesScreen(
    notes: LiveData<List<Note>>,
    lifecycleOwner: LifecycleOwner,
    onAdd: () -> Unit,
    onEdit: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onShowQR: (Note) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    var items by remember { mutableStateOf(emptyList<Note>()) }
    var loaded by remember { mutableStateOf(false) }
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val back: () -> Unit = onBack ?: { dispatcher?.onBackPressed() }

    DisposableEffect(notes, lifecycleOwner) {
        val observer = Observer<List<Note>> {
            items = it.orEmpty().sortedByDescending { note -> note.createdOn }
            loaded = true
        }
        notes.observe(lifecycleOwner, observer)
        onDispose {
            notes.removeObserver(observer)
        }
    }

    TrailSensePage(
        title = stringResource(R.string.tool_notes_title),
        onBack = back,
        backLabel = stringResource(R.string.back),
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                modifier = Modifier.testTag("add_btn"),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add)) }
            )
        }
    ) {
        if (!loaded) return@TrailSensePage

        if (items.isEmpty()) {
            item(key = "empty") {
                TrailSenseEmptyState(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = stringResource(R.string.notes_empty_text),
                    description = stringResource(R.string.notes_empty_description),
                    modifier = Modifier
                        .heightIn(max = 480.dp)
                        .testTag("notes_empty_text")
                )
            }
        } else {
            item(key = "notes") {
                TrailSenseRowGroup(modifier = Modifier.testTag("note_list")) {
                    items.forEach { note ->
                        row { position ->
                            NoteRow(
                                note = note,
                                position = position,
                                onEdit = onEdit,
                                onDelete = onDelete,
                                onShowQR = onShowQR
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NoteRow(
    note: Note,
    position: TrailSenseGroupPosition,
    onEdit: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onShowQR: (Note) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    val title = if (note.title?.trim().isNullOrEmpty()) {
        stringResource(android.R.string.untitled)
    } else {
        note.title.orEmpty().trim()
    }
    val timestamp = remember(note.createdOn) { formatTimestamp(note.createdOn) }
    val preview = remember(note.contents) {
        note.contents.orEmpty().trim().replace(Regex("\\s+"), " ")
    }
    val subtitle = listOfNotNull(timestamp, preview.ifEmpty { null }).joinToString(" · ")

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "notePress"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = trailSenseGroupShape(position),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    role = Role.Button,
                    onClick = { onEdit(note) }
                )
                .padding(
                    start = TrailSenseSpacing.lg,
                    end = TrailSenseSpacing.xs,
                    top = TrailSenseSpacing.md,
                    bottom = TrailSenseSpacing.md
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(TrailSenseShape.iconContainer),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(TrailSenseSize.leadingIconContainer)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = rememberVectorPainter(TrailIcons.ToolNotes),
                        contentDescription = null,
                        modifier = Modifier.size(TrailSenseSize.leadingIcon)
                    )
                }
            }
            Spacer(Modifier.width(TrailSenseSpacing.md))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box {
                IconButton(onClick = { isMenuOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.note_more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.qr_code)) },
                        leadingIcon = {
                            Icon(rememberVectorPainter(TrailIcons.QrCode), contentDescription = null)
                        },
                        onClick = {
                            isMenuOpen = false
                            onShowQR(note)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        leadingIcon = {
                            Icon(rememberVectorPainter(TrailIcons.Delete), contentDescription = null)
                        },
                        onClick = {
                            isMenuOpen = false
                            onDelete(note)
                        }
                    )
                }
            }
        }
    }
}

private val timestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

private fun formatTimestamp(epochMillis: Long): String? {
    if (epochMillis <= 0L) return null
    return runCatching {
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(timestampFormatter)
    }.getOrNull()
}
