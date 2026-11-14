package com.kylecorry.trail_sense.shared.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.views.compose.trailSenseStatusColors
import com.kylecorry.trail_sense.tools.diagnostics.status.StatusBadge

data class SensorStatusItem(val badge: StatusBadge, val label: String, val icon: ImageVector)

@Composable
fun SensorStatusContent(
    items: List<SensorStatusItem>,
    quiet: Boolean,
    opacity: Float,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    if (quiet) {
        QuietSensorStatus(items, onClick, modifier)
    } else {
        Column(
            modifier = modifier.alpha(opacity).clickable(enabled = onClick != null) { onClick?.invoke() },
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            items.filterNot { it.badge.isGood() }.forEach { item ->
                SensorBadgeContent(item.badge, item.label, item.icon)
            }
        }
    }
}

@Composable
private fun QuietSensorStatus(items: List<SensorStatusItem>, onClick: (() -> Unit)?, modifier: Modifier) {
    if (items.isEmpty()) return
    val trouble = items.filterNot { it.badge.isGood() }
    val worst = trouble.firstOrNull { !it.badge.caution() } ?: trouble.firstOrNull() ?: items.first()
    val scheme = MaterialTheme.colorScheme
    val dot = when {
        trouble.isEmpty() -> null
        worst.badge.caution() -> scheme.tertiary
        else -> scheme.error
    }
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            onClick = { open = true },
            shape = CircleShape,
            color = Color.Transparent,
            contentColor = if (dot == null) scheme.onSurfaceVariant else scheme.onSurface,
            modifier = Modifier.size(52.dp).testTag("sensor_status").semantics {
                contentDescription = items.joinToString { "${it.label} ${it.badge.name}" }
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(worst.icon, contentDescription = null, modifier = Modifier.size(22.dp))
                if (dot != null) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp)
                            .size(11.dp).background(scheme.background, CircleShape)
                            .padding(1.5.dp).background(dot, CircleShape)
                    )
                }
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items.forEach { item ->
                val color = when {
                    item.badge.isGood() -> scheme.primary
                    item.badge.caution() -> scheme.tertiary
                    else -> scheme.error
                }
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(item.label, style = MaterialTheme.typography.titleMedium)
                            Text(item.badge.name, style = MaterialTheme.typography.bodyMedium, color = color)
                        }
                    },
                    leadingIcon = { Icon(item.icon, contentDescription = null, tint = color) },
                    onClick = { open = false; onClick?.invoke() }
                )
            }
        }
    }
}

@Composable
private fun SensorBadgeContent(badge: StatusBadge, label: String, icon: ImageVector) {
    val status = trailSenseStatusColors()
    val caution = badge.caution()
    Surface(
        color = if (caution) status.cautionContainer else MaterialTheme.colorScheme.errorContainer,
        contentColor = if (caution) status.onCautionContainer else MaterialTheme.colorScheme.onErrorContainer,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("$label · ${badge.name}", style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun StatusBadge.isGood() = isHealthy || color == AppColor.Green.color
private fun StatusBadge.caution() = color == AppColor.Yellow.color || color == AppColor.Orange.color
