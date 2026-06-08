package com.kylecorry.trail_sense.shared.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconIcon

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
            .testTag("beacon_icon_picker")
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
            MaterialTheme.colorScheme.surface
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(icon?.icon ?: R.drawable.bubble),
                contentDescription = null,
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
