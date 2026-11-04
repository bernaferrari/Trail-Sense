@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The calm empty state: the icon on a badge inside a still cookie, in the feature's [tone], a headline, one line of warm copy, and at most one filled primary action. Screens with
 * an empty state show no second "add" button (no FAB) beside it. Sits in the upper half rather than floating in the
 * middle of a tall screen.
 */
@Composable
fun TrailSenseEmptyState(
    icon: ImageVector,
    title: String,
    description: String? = null,
    action: (@Composable ColumnScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null,
    actionModifier: Modifier = Modifier,
    tone: TrailSenseTone = TrailSenseTone.Primary,
    /** Ignored: the calm empty state has no satellites. Kept so callers compile; remove when passing it. */
    @Suppress("UNUSED_PARAMETER") accents: List<ImageVector> = emptyList(),
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val colors = trailSenseToneColors(tone)
        // Fills the space it is given and centres in it (in a list it simply wraps its content).
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = TrailSenseSpacing.xl, vertical = TrailSenseSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .size(144.dp),
                shape = MaterialShapes.Cookie12Sided.toShape(),
                color = colors.container
            ) {}
            Surface(
                modifier = Modifier.size(88.dp),
                shape = MaterialShapes.Cookie9Sided.toShape(),
                color = colors.accent,
                contentColor = colors.onAccent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp))
                }
            }
        }
        Spacer(Modifier.height(TrailSenseSpacing.xl))
        Column(
            Modifier.widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(TrailSenseSpacing.xl))
            Button(
                onClick = onAction,
                shapes = ButtonDefaults.shapes(),
                modifier = actionModifier.heightIn(min = 56.dp),
                contentPadding = ButtonDefaults.contentPaddingFor(56.dp)
            ) {
                if (actionIcon != null) {
                    Icon(actionIcon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.iconSizeFor(56.dp)))
                    Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(56.dp)))
                }
                Text(actionLabel, style = MaterialTheme.typography.titleMedium)
            }
        }
        if (secondaryActionLabel != null && onSecondaryAction != null) {
            Spacer(Modifier.height(TrailSenseSpacing.xs))
            TextButton(onClick = onSecondaryAction, shapes = ButtonDefaults.shapes()) {
                Text(secondaryActionLabel, style = MaterialTheme.typography.titleSmall)
            }
        }
        if (action != null) {
            Spacer(Modifier.height(TrailSenseSpacing.xl))
            action()
        }
    }
}
