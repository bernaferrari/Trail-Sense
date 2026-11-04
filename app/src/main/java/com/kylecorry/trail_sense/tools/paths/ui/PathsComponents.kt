package com.kylecorry.trail_sense.tools.paths.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

/**
 * Lets one lazy item per row still read as one group inside a TrailSensePage, whose list puts 16 dp between items:
 * every row after the first is pulled up so rows sit 2 dp apart, the family's group gap. Long path lists stay lazy
 * instead of composing every row in a single section item.
 */
internal fun Modifier.pathsGroupedRowGap(isFirst: Boolean): Modifier {
    if (isFirst) return this
    return layout { measurable, constraints ->
        val pull = (TrailSenseSpacing.lg - TrailSenseSpacing.xxs).roundToPx()
        val placeable = measurable.measure(constraints)
        layout(placeable.width, (placeable.height - pull).coerceAtLeast(0)) {
            placeable.place(0, -pull)
        }
    }
}
