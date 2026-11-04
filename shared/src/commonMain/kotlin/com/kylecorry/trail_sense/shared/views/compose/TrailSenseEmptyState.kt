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
import androidx.compose.foundation.layout.fillMaxWidth
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
 * The family's empty state: a layered expressive mark (a slowly turning cookie, the icon on a primary badge, small
 * shapes drifting round it), a headline, one line of warm copy, and at most one filled primary action. Screens with
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
    actionModifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "empty")
    val turn by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(36_000, easing = LinearEasing)), label = "turn")
    val bob by transition.animateFloat(-4f, 4f, infiniteRepeatable(tween(2_600), RepeatMode.Reverse), label = "bob")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TrailSenseSpacing.xl, vertical = TrailSenseSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(208.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .size(176.dp)
                    .rotate(turn),
                shape = MaterialShapes.Cookie12Sided.toShape(),
                color = scheme.primaryContainer
            ) {}
            Surface(
                modifier = Modifier.size(104.dp),
                shape = MaterialShapes.Cookie9Sided.toShape(),
                color = scheme.primary,
                contentColor = scheme.onPrimary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp))
                }
            }
            Satellite(MaterialShapes.Sunny.toShape(), scheme.tertiaryContainer, 36.dp, (-78).dp, (-70 + bob).dp)
            Satellite(MaterialShapes.Flower.toShape(), scheme.secondaryContainer, 28.dp, 82.dp, (60 - bob).dp)
            Satellite(MaterialShapes.Pill.toShape(), scheme.tertiary, 16.dp, 86.dp, (-58 - bob).dp)
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
        if (action != null) {
            Spacer(Modifier.height(TrailSenseSpacing.xl))
            action()
        }
    }
}

@Composable
private fun BoxScope.Satellite(shape: Shape, color: Color, size: Dp, x: Dp, y: Dp) {
    Surface(
        modifier = Modifier
            .align(Alignment.Center)
            .offset(x = x, y = y)
            .size(size),
        shape = shape,
        color = color
    ) {}
}
