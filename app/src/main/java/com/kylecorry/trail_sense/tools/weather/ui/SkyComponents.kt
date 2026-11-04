@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.weather.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape

/*
 * Small pieces shared by the sky tools (weather, astronomy, climate, clouds, lightning and their neighbours), so the
 * charts, rows and focal shapes read as one family across them.
 */

/** A still expressive shape behind an icon (or a short text): only ever the screen's live object or empty state. */
@Composable
internal fun SkyFocalShape(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    text: String? = null,
    shape: Shape = MaterialShapes.Cookie12Sided.toShape(),
    size: Dp = 96.dp,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    iconRotation: Float = 0f
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .background(containerColor, shape)
        )
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .size(size * 0.44f)
                    .graphicsLayer { rotationZ = iconRotation }
            )
        } else if (text != null) {
            Text(
                text,
                color = contentColor,
                style = (if (text.length <= 3) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge)
                    .copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

/**
 * A row in a group: a leading icon in the sky tools' one shape (a quiet secondary cookie), a title, and a SemiBold
 * trailing value. [iconContainerColor] changes only for meaning (an error row), never for decoration.
 */
@Composable
internal fun SkyValueRow(
    title: String,
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    icon: ImageVector? = null,
    // The family row: a 12% primary Cookie6 well, titleMedium, surfaceContainer, a quiet value on the right.
    iconContainerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    iconContentColor: Color = MaterialTheme.colorScheme.primary,
    iconRotation: Float = 0f,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = trailSenseGroupShape(position),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .heightIn(min = 72.dp)
                .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                TrailSenseShapeBadge(
                    icon = icon,
                    shape = MaterialShapes.Cookie6Sided.toShape(),
                    size = 40.dp,
                    containerColor = iconContainerColor,
                    contentColor = iconContentColor,
                    iconRotation = iconRotation
                )
                Spacer(Modifier.width(TrailSenseSpacing.md))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (value != null) {
                Spacer(Modifier.width(TrailSenseSpacing.md))
                Text(
                    value,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Medium,
                    color = valueColor,
                    textAlign = TextAlign.End,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 200.dp)
                )
            }
            trailing?.invoke(this)
        }
    }
}

/** A small pill of status (a tendency, a phase, a count) on any surface. */
@Composable
internal fun SkyPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    iconRotation: Float = 0f
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            Modifier.padding(start = if (icon != null) 10.dp else 12.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer { rotationZ = iconRotation }
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/**
 * A value callout that floats over a chart's selected point: an inverse pill centred on [fraction] of the width,
 * kept inside the chart's edges.
 */
@Composable
internal fun SkyChartCallout(fraction: Float, text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    androidx.compose.ui.layout.Layout(
        content = {
            SkyPill(
                text = text,
                icon = icon,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        },

        modifier = modifier.fillMaxWidth()
    ) { measurables, constraints ->
        val pill = measurables.first().measure(constraints.copy(minWidth = 0))
        val width = constraints.maxWidth
        layout(width, pill.height) {
            val x = (fraction.coerceIn(0f, 1f) * width - pill.width / 2f).toInt()
                .coerceIn(0, (width - pill.width).coerceAtLeast(0))
            pill.place(x, 0)
        }
    }
}
