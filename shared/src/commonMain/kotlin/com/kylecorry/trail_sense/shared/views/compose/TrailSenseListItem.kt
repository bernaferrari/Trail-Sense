package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Modern, expressive list item — Material 3 Expressive style.
 * Supports leading icon, rich content, and trailing elements.
 */
@Composable
fun TrailSenseListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: Painter? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconRotation: Float = 0f,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    animateEntrance: Boolean = true
) {
    var visible by remember { mutableStateOf(!animateEntrance) }

    LaunchedEffect(Unit) {
        if (animateEntrance) {
            delay(40)
            visible = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = TrailSenseAnimation.fastFade,
        label = "listItemEntrance"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(
                horizontal = TrailSenseSpacing.lg,
                vertical = TrailSenseSpacing.md
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(iconRotation)
            )
            Spacer(Modifier.width(TrailSenseSpacing.md))
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (trailing != null) {
            Spacer(Modifier.width(TrailSenseSpacing.md))
            trailing()
        }
    }
}
