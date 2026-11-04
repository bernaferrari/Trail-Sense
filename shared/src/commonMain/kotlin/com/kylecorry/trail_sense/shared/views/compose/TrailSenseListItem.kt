package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * The family's list row. Every grouped list follows this spec:
 * - 16 dp side padding, 12 dp top and bottom; at least 56 dp tall with one line, 72 dp with a supporting line.
 * - An optional leading icon in a 40 dp, 12 dp-radius tile tinted 12% primary (the family's well). Use icons only when they differ in
 *   meaning; if every row would get the same icon, pass none.
 * - Title in titleMedium onSurface; one supporting line in bodyMedium onSurfaceVariant (at most two lines).
 * - At most one trailing element: either [trailingText] (a short value, bodyLarge onSurfaceVariant) or [trailing]
 *   (one switch, icon button or chevron), never both.
 * - State colour comes from the caller only for real meaning: error for danger, tertiary for attention.
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
    trailingText: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    position: TrailSenseGroupPosition? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    // Off by default: in a LazyColumn this would re-fire every time a row scrolls
    // back into view, which reads as jank. Opt in for short, static lists only.
    animateEntrance: Boolean = false
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.985f else 1f,
        animationSpec = TrailSenseAnimation.quickSpring,
        label = "listItemPress"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        color = if (position != null) containerColor else Color.Transparent,
        shape = position?.let(::trailSenseGroupShape) ?: MaterialTheme.shapes.extraSmall
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (subtitle != null) 72.dp else 56.dp)
                .then(
                    if (onClick != null) {
                        Modifier.combinedClickable(
                            interactionSource = interactionSource,
                            indication = androidx.compose.material3.ripple(),
                            role = Role.Button,
                            onClick = onClick,
                            onLongClick = onLongClick
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(
                    horizontal = TrailSenseSpacing.lg,
                    vertical = TrailSenseSpacing.md
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = iconTint.copy(alpha = 0.12f),
                    contentColor = iconTint,
                    modifier = Modifier.size(40.dp)
                ) {
                    androidx.compose.foundation.layout.Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier
                                .size(TrailSenseSize.leadingIcon)
                                .rotate(iconRotation)
                        )
                    }
                }
                Spacer(Modifier.width(TrailSenseSpacing.md))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (trailingText != null) {
                Spacer(Modifier.width(TrailSenseSpacing.md))
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            } else if (trailing != null) {
                Spacer(Modifier.width(TrailSenseSpacing.md))
                trailing()
            }
        }
    }
}
