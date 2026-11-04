package com.kylecorry.trail_sense.shared.views

import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconIcon
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

@Composable
fun BeaconIconPickerDialog(
    title: String,
    selectedIcon: BeaconIcon?,
    icons: List<BeaconIcon>,
    onIconPicked: (BeaconIcon?) -> Unit,
    onDismiss: () -> Unit
) {
    var icon by remember(selectedIcon) { mutableStateOf(selectedIcon) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title)
        },
        text = {
            BeaconIconPickerContent(
                selectedIcon = icon,
                icons = icons,
                onIconSelected = {
                    icon = it
                }
            )
        },
        confirmButton = {
            TextButton(onClick = { onIconPicked(icon) }) {
                Text(text = androidx.compose.ui.res.stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = androidx.compose.ui.res.stringResource(android.R.string.cancel))
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BeaconIconPickerContent(
    selectedIcon: BeaconIcon?,
    icons: List<BeaconIcon>,
    onIconSelected: (BeaconIcon?) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .testTag("beacon_icon_picker"),
        horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
    ) {
        BeaconIconButton(
            icon = null,
            selected = selectedIcon == null,
            onClick = { onIconSelected(null) }
        )
        icons.forEach { icon ->
            BeaconIconButton(
                icon = icon,
                selected = icon == selectedIcon,
                onClick = { onIconSelected(icon) }
            )
        }
    }
}

@Composable
private fun BeaconIconButton(
    icon: BeaconIcon?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(64.dp)
            .clickable(onClick = onClick)
            .testTag("beacon_icon_${icon?.name ?: "none"}"),
        shape = CircleShape,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = rememberVectorPainter(icon?.icon ?: TrailIcons.Bubble),
                contentDescription = icon?.name ?: stringResource(R.string.none),
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
