@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.convert.ui

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
            val fade = MaterialTheme.colorScheme.surface
            Row(
                modifier = Modifier
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        // Fade the far edge so it's clear more categories scroll in.
                        drawRect(
                            Brush.horizontalGradient(
                                0.88f to Color.Black,
                                1f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
                names.forEachIndexed { index, name ->
                    val selected = selectedIndex == index
                    FilterChip(
                        selected = selected,
                        onClick = { selectedIndex = index },
                        label = { Text(name, style = MaterialTheme.typography.labelLarge) },
                        leadingIcon = (if (selected) Icons.Rounded.Check else icons.getOrNull(index))?.let { icon ->
                            {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        },
                        modifier = Modifier.heightIn(min = 52.dp),
                        shape = when (index) {
                            0 -> RoundedCornerShape(topStart = 26.dp, bottomStart = 26.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                            names.lastIndex -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 26.dp, bottomEnd = 26.dp)
                            else -> RoundedCornerShape(8.dp)
                        },
                        border = null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
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
