@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.beacons.ui.places

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape

/*
 * Pieces shared by the places and planning tools (beacons, paths, maps, tides, packs, notes): a warm empty state,
 * a rich list row with a shaped leading badge and a trailing value that means something, a mini bar, and a way to
 * lay grouped rows out one lazy item each while they still read as one connected group.
 */

/** The spacing TrailSensePage puts between lazy items, and the gap rows of a group keep between them. */
private val PageItemSpacing = TrailSenseSpacing.lg
private val GroupGap = TrailSenseSpacing.xxs

/**
 * Lets a row overhang into the page's item spacing so consecutive rows sit 2 dp apart like a group, while each row
 * stays its own lazy item (long lists only compose what's on screen).
 */
fun Modifier.placesJoinNext(joined: Boolean): Modifier = if (!joined) this else layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val overlap = (PageItemSpacing - GroupGap).roundToPx()
    layout(placeable.width, (placeable.height - overlap).coerceAtLeast(0)) {
        placeable.place(0, 0)
    }
}

/**
 * A group of rows laid out lazily: an optional header item, then one item per row with its group position, joined
 * into one silhouette with [placesJoinNext].
 */
inline fun <T> LazyListScope.placesGroup(
    key: String,
    items: List<T>,
    crossinline itemKey: (T) -> Any,
    title: String? = null,
    crossinline row: @Composable (item: T, position: TrailSenseGroupPosition, modifier: Modifier) -> Unit
) {
    if (items.isEmpty()) return
    if (title != null) {
        item(key = "$key-header") {
            TrailSenseGroupHeader(title, modifier = Modifier.placesJoinNext(true))
        }
    }
    items.forEachIndexed { index, value ->
        item(key = "$key-${itemKey(value)}") {
            val position = TrailSenseGroupPosition.at(index, items.size)
            row(value, position, Modifier.placesJoinNext(!position.isLast))
        }
    }
}

/**
 * A calm screen's answer: its one key value in displaySmall SemiBold onSurface at the top of the content, with one
 * verdict line under it. No card, no badge, no eyebrow. [below] holds an optional quiet extra (a progress bar).
 */
@Composable
fun PlacesKeyValue(
    value: String,
    modifier: Modifier = Modifier,
    verdict: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    /** displaySmall for a value; a sentence (the next step, before there is a value) reads better at headlineSmall. */
    valueStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.displaySmall,
    below: (@Composable ColumnScope.() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
    ) {
        Text(
            value,
            style = valueStyle.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
        if (verdict != null) {
            Text(
                verdict,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (below != null) {
            Spacer(Modifier.height(TrailSenseSpacing.xs))
            below()
        }
    }
}

/** A thin rounded bar for "how much" next to a value (proximity, progress, share of a total). */
@Composable
fun PlacesMiniBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    width: Dp = 64.dp,
    height: Dp = 8.dp
) {
    val animated by animateFloatAsState(
        fraction.coerceIn(0f, 1f),
        MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "miniBar"
    )
    Box(
        modifier
            .width(width)
            .height(height)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        if (animated > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(animated.coerceAtLeast(0.08f))
                    .height(height)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/**
 * The places list row: an expressive leading badge, the title and one line of context, and a trailing value that
 * carries the row's meaning (distance, length, packed count) with an optional mini bar under it. [actions] sit at
 * the very end (visibility toggle, overflow menu).
 */
@Composable
fun PlacesRow(
    title: String,
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    value: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    valueCaption: String? = null,
    barFraction: Float? = null,
    barColor: Color = MaterialTheme.colorScheme.primary,
    dimmed: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    belowContent: (@Composable ColumnScope.() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && onClick != null) 0.985f else 1f,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "rowPress"
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = trailSenseGroupShape(position),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .then(
                    if (onClick != null) {
                        Modifier.combinedClickable(
                            interactionSource = interaction,
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
                    start = TrailSenseSpacing.lg,
                    end = if (actions != null) TrailSenseSpacing.xs else TrailSenseSpacing.lg,
                    top = TrailSenseSpacing.md,
                    bottom = TrailSenseSpacing.md
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                Box(Modifier.graphicsLayer { alpha = if (dimmed) 0.55f else 1f }) { leading() }
                Spacer(Modifier.width(TrailSenseSpacing.md))
            }
            Column(
                Modifier
                    .weight(1f)
                    .graphicsLayer { alpha = if (dimmed) 0.6f else 1f },
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                belowContent?.invoke(this)
            }
            if (value != null) {
                Spacer(Modifier.width(TrailSenseSpacing.md))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
                ) {
                    Text(
                        value,
                        style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        color = valueColor,
                        maxLines = 1
                    )
                    if (barFraction != null) {
                        PlacesMiniBar(barFraction, color = barColor)
                    }
                    if (valueCaption != null) {
                        Text(
                            valueCaption,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
            if (actions != null) {
                Spacer(Modifier.width(TrailSenseSpacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically, content = actions)
            }
        }
    }
}

/** A list row's leading mark: an icon (or short text) inside an expressive shape, in a given colour. */
@Composable
fun PlacesBadge(
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    text: String? = null,
    shape: Shape = MaterialShapes.Cookie9Sided.toShape(),
    size: Dp = 40.dp,
    iconRotation: Float = 0f,
    iconScale: Float = 0.55f
) {
    Surface(modifier = modifier.size(size), shape = shape, color = containerColor, contentColor = contentColor) {
        Box(contentAlignment = Alignment.Center) {
            when {
                icon != null -> Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(size * iconScale)
                        .rotate(iconRotation)
                )

                text != null -> Text(
                    text,
                    style = (if (text.length <= 2) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge)
                        .copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}
