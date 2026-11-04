@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The meaning behind a spotlight, tile or badge colour, so a screen says "good" rather than picking a green. */
enum class TrailSenseTone { Primary, Secondary, Tertiary, Good, Caution, Error, Neutral }

/** A container colour and its content colour, plus the strong accent used for badges and bars on it. */
@Immutable
data class TrailSenseToneColors(val container: Color, val content: Color, val accent: Color, val onAccent: Color)

@Composable
fun trailSenseToneColors(tone: TrailSenseTone): TrailSenseToneColors {
    val scheme = MaterialTheme.colorScheme
    val status = trailSenseStatusColors()
    return when (tone) {
        TrailSenseTone.Primary -> TrailSenseToneColors(scheme.primaryContainer, scheme.onPrimaryContainer, scheme.primary, scheme.onPrimary)
        TrailSenseTone.Secondary -> TrailSenseToneColors(scheme.secondaryContainer, scheme.onSecondaryContainer, scheme.secondary, scheme.onSecondary)
        TrailSenseTone.Tertiary -> TrailSenseToneColors(scheme.tertiaryContainer, scheme.onTertiaryContainer, scheme.tertiary, scheme.onTertiary)
        TrailSenseTone.Good -> TrailSenseToneColors(status.goodContainer, status.onGoodContainer, status.good, status.goodContainer)
        TrailSenseTone.Caution -> TrailSenseToneColors(status.cautionContainer, status.onCautionContainer, status.caution, status.onCautionContainer)
        TrailSenseTone.Error -> TrailSenseToneColors(scheme.errorContainer, scheme.onErrorContainer, scheme.error, scheme.onError)
        TrailSenseTone.Neutral -> TrailSenseToneColors(scheme.surfaceContainerLow, scheme.onSurface, scheme.primary, scheme.onPrimary)
    }
}

/**
 * A short value or an icon inside one of Material's expressive shapes: the cookie, sunny or flower badges the family
 * uses for "37", a check, or a direction. Text shrinks to fit the shape.
 */
@Composable
fun TrailSenseShapeBadge(
    modifier: Modifier = Modifier,
    text: String? = null,
    icon: ImageVector? = null,
    shape: Shape = MaterialShapes.Cookie9Sided.toShape(),
    size: Dp = 56.dp,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    iconRotation: Float = 0f
) {
    val container by animateColorAsState(containerColor, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "badge")
    Surface(
        modifier = modifier.size(size),
        shape = shape,
        color = container,
        contentColor = contentColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                icon != null -> Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(size * 0.46f)
                        .rotate(iconRotation)
                )

                text != null -> Text(
                    text = text,
                    style = when {
                        text.length <= 2 -> MaterialTheme.typography.titleLarge
                        text.length <= 4 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.labelLarge
                    }.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


/** A button inside a [TrailSenseSpotlight]; the first is the main one. */
@Immutable
data class TrailSenseSpotlightAction(
    val label: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit
)

/**
 * The family's hero: one saturated surface holding the screen's single most important state ("Protected",
 * "0 lx", "Camp Muir"), an expressive badge beside it, a line of context, and the actions that change that state
 * as a connected pair of buttons. Everything else on the screen sits quietly below it.
 */
@Composable
fun TrailSenseSpotlight(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    supportingText: String? = null,
    tone: TrailSenseTone = TrailSenseTone.Primary,
    badge: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    actions: List<TrailSenseSpotlightAction> = emptyList(),
    titleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineLarge,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val colors = trailSenseToneColors(tone)
    val container by animateColorAsState(colors.container, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "spot")
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
        contentColor = colors.content
    ) {
        Column(
            modifier = Modifier
                .padding(TrailSenseSpacing.xl)
                .animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (badge != null) {
                    badge()
                    Spacer(Modifier.width(TrailSenseSpacing.lg))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (eyebrow != null) {
                        Text(
                            eyebrow,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.content.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        title,
                        style = titleStyle.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold
                    )
                    if (supportingText != null) {
                        Text(
                            supportingText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.content.copy(alpha = 0.82f)
                        )
                    }
                }
                if (trailing != null) {
                    Spacer(Modifier.width(TrailSenseSpacing.sm))
                    trailing()
                }
            }
            content?.invoke(this)
            if (actions.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                    actions.forEachIndexed { index, action ->
                        val first = index == 0
                        val last = index == actions.lastIndex
                        val outer = 28.dp
                        val inner = 8.dp
                        FilledTonalButton(
                            onClick = action.onClick,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                topStart = if (first) outer else inner,
                                bottomStart = if (first) outer else inner,
                                topEnd = if (last) outer else inner,
                                bottomEnd = if (last) outer else inner
                            ),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.72f),
                                contentColor = colors.content
                            )
                        ) {
                            if (action.icon != null) {
                                Icon(action.icon, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(TrailSenseSpacing.sm))
                            }
                            Text(action.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

/** One value tile in a [TrailSenseStatGrid]. */
@Immutable
data class TrailSenseStat(
    val label: String,
    val value: String,
    val supportingText: String? = null,
    val tone: TrailSenseTone = TrailSenseTone.Neutral,
    val icon: ImageVector? = null,
    val onClick: (() -> Unit)? = null
)

/**
 * Values as connected tiles, two or three to a row (SDKMonitor's target and minimum SDK, Calcula's statistics): a
 * small label, a large tabular value and an optional line under it. A tile with a tone stands out in its colour.
 */
@Composable
fun TrailSenseStatGrid(
    stats: List<TrailSenseStat>,
    modifier: Modifier = Modifier,
    columns: Int = if (stats.size == 3) 3 else 2,
    valueStyle: androidx.compose.ui.text.TextStyle = if (columns >= 3) {
        MaterialTheme.typography.titleLarge
    } else {
        MaterialTheme.typography.headlineSmall
    }
) {
    if (stats.isEmpty()) return
    val rows = stats.chunked(columns)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
        rows.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                row.forEachIndexed { columnIndex, stat ->
                    val colors = trailSenseToneColors(stat.tone)
                    val shape = trailSenseGridCellShape(columnIndex, row.size, rowIndex, rows.size)
                    val body: @Composable () -> Unit = {
                        Column(
                            Modifier
                                .heightIn(min = 88.dp)
                                .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (stat.icon != null) {
                                    Icon(
                                        stat.icon,
                                        contentDescription = null,
                                        tint = if (stat.tone == TrailSenseTone.Neutral) MaterialTheme.colorScheme.primary else colors.content,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(TrailSenseSpacing.xs))
                                }
                                Text(
                                    stat.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (stat.tone == TrailSenseTone.Neutral) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        colors.content.copy(alpha = 0.8f)
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                stat.value,
                                style = valueStyle.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (stat.supportingText != null) {
                                Text(
                                    stat.supportingText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (stat.tone == TrailSenseTone.Neutral) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        colors.content.copy(alpha = 0.8f)
                                    },
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (stat.onClick != null) {
                        Surface(
                            onClick = stat.onClick,
                            modifier = Modifier.weight(1f),
                            shape = shape,
                            color = colors.container,
                            contentColor = colors.content
                        ) { body() }
                    } else {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = shape,
                            color = colors.container,
                            contentColor = colors.content
                        ) { body() }
                    }
                }
            }
        }
    }
}
