@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package com.kylecorry.trail_sense.shared.views.compose

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.TrailSenseIcons

/*
 * Instrument kit: the pieces every full-bleed tool (level, clinometer, ruler, flashlight, magnifier…) shares, so
 * they read as one family. Lives beside the level for now; it belongs with the rest of the kit in
 * shared/views/compose once that module is free to edit.
 */

/** Goes back the same way the system back gesture does; for screens the host doesn't hand a back callback. */
@Composable
fun rememberTrailSenseBack(): () -> Unit {
    val owner = LocalOnBackPressedDispatcherOwner.current
    return remember(owner) { { owner?.onBackPressedDispatcher?.onBackPressed() } }
}

/** The small top bar of an instrument: back arrow, one-line title, actions. Sits on [containerColor]. */
@Composable
fun TrailSenseInstrumentTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = rememberTrailSenseBack(),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(TrailSenseIcons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
        },
        actions = actions,
        // The activity already pads the status bar.
        windowInsets = WindowInsets(0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = containerColor,
            titleContentColor = contentColor,
            navigationIconContentColor = contentColor,
            actionIconContentColor = contentColor
        )
    )
}

/**
 * A full-bleed instrument: the small top bar, then the instrument filling the rest of the screen. Unlike
 * [TrailSensePage] nothing scrolls by default and nothing is width-capped, so camera previews and dials can reach
 * the edges; the instrument decides its own gutters.
 */
@Composable
fun TrailSenseInstrumentScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = rememberTrailSenseBack(),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    Surface(modifier = modifier.fillMaxSize(), color = containerColor, contentColor = contentColor) {
        Column(modifier = Modifier.fillMaxSize()) {
            TrailSenseInstrumentTopBar(
                title = title,
                onBack = onBack,
                containerColor = containerColor,
                contentColor = contentColor,
                actions = actions
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                content = content
            )
        }
    }
}

/**
 * The hero reading of an instrument: the value large in tabular figures (so digits don't jitter while they change),
 * the unit smaller beside it. Pass `"--"` while the first value is on its way so the layout holds still.
 */
@Composable
fun TrailSenseReading(
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    label: String? = null,
    style: TextStyle = MaterialTheme.typography.displayLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    unitColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = unitColor,
                textAlign = TextAlign.Center
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = style.copy(fontFeatureSettings = "tnum"),
                color = color,
                maxLines = 1
            )
            if (!unit.isNullOrBlank()) {
                Spacer(Modifier.width(TrailSenseSpacing.sm))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.titleMedium,
                    color = unitColor,
                    modifier = Modifier.padding(bottom = TrailSenseSpacing.sm),
                    maxLines = 1
                )
            }
        }
    }
}

/** One cell of a [TrailSenseMetricRow]. */
data class TrailSenseMetricItem(val label: String, val value: String, val unit: String? = null)

/**
 * Secondary readings as one compact tonal strip: equal columns, label above a tabular value. Use for the two to
 * four numbers that sit under a hero reading.
 */
@Composable
fun TrailSenseMetricRow(
    items: List<TrailSenseMetricItem>,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    position: TrailSenseGroupPosition = TrailSenseGroupPosition.Single
) {
    if (items.isEmpty()) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        shape = trailSenseGroupShape(position)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = TrailSenseSpacing.sm,
                vertical = TrailSenseSpacing.lg
            )
        ) {
            items.forEach { item ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = TrailSenseSpacing.sm)
                        .semantics(mergeDescendants = true) {},
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = item.value,
                            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        if (!item.unit.isNullOrBlank()) {
                            Spacer(Modifier.width(TrailSenseSpacing.xs))
                            Text(
                                text = item.unit,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = TrailSenseSpacing.xxs),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A floating tonal panel for controls laid over a camera or a dial (bottom bars of full-bleed instruments). */
@Composable
fun TrailSenseInstrumentPanel(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(TrailSenseShape.groupOuter),
        color = containerColor,
        content = content
    )
}

/** The family's press feedback for tiles and big buttons: a quiet shrink while held. */
@Composable
fun Modifier.trailSensePressScale(interactionSource: InteractionSource, pressedScale: Float = 0.96f): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "pressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Fills the instrument area with a centered empty state (no sensor, no permission). */
@Composable
fun BoxScope.TrailSenseInstrumentMessage(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
