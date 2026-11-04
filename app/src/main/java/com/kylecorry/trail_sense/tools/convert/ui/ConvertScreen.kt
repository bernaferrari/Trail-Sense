@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.convert.ui

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.rememberTrailSenseBack

@Composable
fun ConvertScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pages = remember(context) {
        listOf(
            CoordinateConverterPage(context),
            DistanceConverterPage(context),
            TemperatureConverterPage(context),
            VolumeConverterPage(context),
            WeightConverterPage(context),
            TimeConverterPage(context)
        )
    }
    val names = listOf(
        stringResource(R.string.coordinates_tab),
        stringResource(R.string.distance),
        stringResource(R.string.temperature),
        stringResource(R.string.volume),
        stringResource(R.string.weight),
        stringResource(R.string.time)
    )
    ConverterTabs(
        pages = pages,
        names = names,
        icons = listOf(
            TrailIcons.Location,
            TrailIcons.Ruler,
            TrailIcons.Thermometer,
            TrailIcons.Precipitation,
            TrailIcons.Weight,
            TrailIcons.ToolClock
        ),
        modifier = modifier
    )
}

/**
 * Convert as a standard page: what you're converting is a row of chips (six don't fit a segmented control), and
 * the chosen converter fades in beneath it.
 */
@Composable
fun ConverterTabs(
    pages: List<ConverterPage>,
    names: List<String>,
    modifier: Modifier = Modifier,
    icons: List<ImageVector?> = emptyList()
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val fadeOutSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()

    TrailSensePage(
        title = stringResource(R.string.convert),
        onBack = rememberTrailSenseBack(),
        backLabel = stringResource(R.string.back),
        modifier = modifier
    ) {
        item("categories") {
            // Six categories as a connected 3 x 2 grid of equal tiles, so rows always line up.
            val columns = 3
            val rows = names.indices.chunked(columns)
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                rows.forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                        row.forEachIndexed { columnIndex, index ->
                            CategoryTile(
                                name = names[index],
                                icon = icons.getOrNull(index),
                                selected = selectedIndex == index,
                                shape = com.kylecorry.trail_sense.shared.views.compose.trailSenseGridCellShape(
                                    columnIndex, row.size, rowIndex, rows.size
                                ),
                                onClick = { selectedIndex = index },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
        item("converter") {
            AnimatedContent(
                targetState = selectedIndex,
                transitionSpec = {
                    fadeIn(fadeInSpec) togetherWith fadeOut(fadeOutSpec)
                },
                label = "converter"
            ) { index ->
                pages[index].Content()
            }
        }
    }
}

@Composable
private fun CategoryTile(
    name: String,
    icon: ImageVector?,
    selected: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val container by androidx.compose.animation.animateColorAsState(
        if (selected) scheme.primary else scheme.surfaceContainerLow,
        MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "categoryTile"
    )
    val content by androidx.compose.animation.animateColorAsState(
        if (selected) scheme.onPrimary else scheme.onSurface,
        MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "categoryTileContent"
    )
    androidx.compose.material3.Surface(
        modifier = modifier
            .heightIn(min = 76.dp)
            .androidxSelectable(selected, onClick),
        shape = shape,
        color = container,
        contentColor = content
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(vertical = TrailSenseSpacing.md, horizontal = TrailSenseSpacing.xs),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs, androidx.compose.ui.Alignment.CenterVertically)
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (selected) content else scheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

private fun Modifier.androidxSelectable(selected: Boolean, onClick: () -> Unit): Modifier =
    this.then(
        Modifier.selectable(
            selected = selected,
            role = androidx.compose.ui.semantics.Role.Tab,
            onClick = onClick
        )
    )
