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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The calm empty state: the icon in one still cookie in the feature's [tone], a headline, one line of warm copy, and at
 * most one filled primary action. Screens with an empty state show no second "add" button (no FAB) beside it.
 *
 * Given a bounded height, the group's centre sits at a fixed optical position, [EmptyStateCentre] (40 %) of the
 * height, on every screen; in a list (unbounded height) it simply wraps its content.
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
    Layout(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = TrailSenseSpacing.xl, vertical = TrailSenseSpacing.xxl),
        content = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    color = colors.container,
                    contentColor = colors.content
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp))
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
                        modifier = actionModifier.heightIn(min = 48.dp),
                        contentPadding = ButtonDefaults.contentPaddingFor(48.dp)
                    ) {
                        if (actionIcon != null) {
                            Icon(actionIcon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.iconSizeFor(48.dp)))
                            Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(48.dp)))
                        }
                        Text(actionLabel, style = MaterialTheme.typography.labelLarge)
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
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
        val height = if (constraints.hasBoundedHeight) constraints.maxHeight else placeable.height
        val y = (height * EmptyStateCentre - placeable.height / 2f).toInt()
            .coerceIn(0, (height - placeable.height).coerceAtLeast(0))
        layout(width, height) {
            placeable.place((width - placeable.width) / 2, y)
        }
    }
}

/** Where the empty state's centre sits, as a fraction of the height it is given (slightly above the middle). */
private const val EmptyStateCentre = 0.4f
