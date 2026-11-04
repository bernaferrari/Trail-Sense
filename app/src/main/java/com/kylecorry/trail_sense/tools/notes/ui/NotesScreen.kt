@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.notes.ui

import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlightAction
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesBadge
import com.kylecorry.trail_sense.tools.beacons.ui.places.placesGroup
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
            // One anchored action for both states: the empty notebook only tells the story, the FAB starts the note.
            if (loaded) ExtendedFloatingActionButton(
                onClick = onAdd,
                modifier = Modifier.testTag("add_btn"),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                icon = { Icon(TrailIcons.Edit, contentDescription = null) },
                text = { Text(stringResource(R.string.places_notes_new)) }
            )
        }
    ) {
        if (!loaded) return@TrailSensePage

        if (items.isEmpty()) {
            item(key = "empty") {
                TrailSenseEmptyState(
                    icon = TrailIcons.ToolNotes,
                    title = stringResource(R.string.notes_empty_text),
                    description = stringResource(R.string.notes_empty_description),
                    // A warm notebook: a pencil, ruled lines and a date ride round the page.
                    tone = TrailSenseTone.Tertiary,
                    accents = listOf(TrailIcons.Edit, TrailIcons.Summary, TrailIcons.Date),
                    modifier = Modifier
                        .fillParentMaxHeight(0.78f)
                        .testTag("notes_empty_text")
                )
            }
        } else {
            val latest = items.first()
            item(key = "latest") {
                LatestNoteSpotlight(note = latest, onEdit = onEdit, onShowQR = onShowQR, onDelete = onDelete)
            }
            val rest = items.drop(1)
            placesGroup(
                key = "notes",
                items = rest,
                itemKey = { it.id }
            ) { note, position, rowModifier ->
                NoteRow(
                    note = note,
                    position = position,
                    modifier = if (position.isFirst) rowModifier.testTag("note_list") else rowModifier,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onShowQR = onShowQR
                )
            }
        }
    }
}

/** The newest note, opened up on a warm card: its title, the start of what it says, and the two things you do with it. */
@Composable
private fun LatestNoteSpotlight(
    note: Note,
    onEdit: (Note) -> Unit,
    onShowQR: (Note) -> Unit,
    onDelete: (Note) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    val title = if (note.title?.trim().isNullOrEmpty()) {
        stringResource(android.R.string.untitled)
    } else {
        note.title.orEmpty().trim()
    }
    val preview = remember(note.contents) {
        note.contents.orEmpty().trim().replace(Regex("\\s+"), " ").let {
            if (it.length > 160) it.take(160).trimEnd() + "…" else it
        }
    }
    val timestamp = remember(note.createdOn) { formatTimestamp(note.createdOn) }
    TrailSenseSpotlight(
        eyebrow = listOfNotNull(stringResource(R.string.places_notes_latest), timestamp).joinToString(" · "),
        title = title,
        titleStyle = MaterialTheme.typography.headlineSmall,
        supportingText = preview.ifEmpty { null },
        tone = TrailSenseTone.Tertiary,
        trailing = {
            Box {
                IconButton(onClick = { isMenuOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.note_more_options)
                    )
                }
                DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        leadingIcon = { Icon(rememberVectorPainter(TrailIcons.Delete), contentDescription = null) },
                        onClick = {
                            isMenuOpen = false
                            onDelete(note)
                        }
                    )
                }
            }
        },
        badge = {
            TrailSenseShapeBadge(
                icon = TrailIcons.ToolNotes,
                shape = MaterialShapes.Cookie7Sided.toShape(),
                size = 56.dp,
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        },
        actions = listOf(
            TrailSenseSpotlightAction(stringResource(R.string.edit), TrailIcons.Edit) { onEdit(note) },
            TrailSenseSpotlightAction(stringResource(R.string.qr_code), TrailIcons.QrCode) { onShowQR(note) }
        )
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NoteRow(
    note: Note,
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier,
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
    val subtitle = preview.ifEmpty { timestamp.orEmpty() }
    val date = remember(note.createdOn) {
        if (note.createdOn <= 0L) null else Instant.ofEpochMilli(note.createdOn).atZone(ZoneId.systemDefault())
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "notePress"
    )

    Surface(
        modifier = modifier
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
            // The day it was written, on a soft cookie, with the month under it: notes read like a dated notebook.
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PlacesBadge(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    text = date?.dayOfMonth?.toString(),
                    icon = if (date == null) TrailIcons.ToolNotes else null,
                    shape = MaterialShapes.Cookie4Sided.toShape(),
                    size = 44.dp
                )
                if (date != null) {
                    Text(
                        date.format(monthFormatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(TrailSenseSpacing.lg))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
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

private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM")

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
