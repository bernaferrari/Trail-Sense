@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.experimentation

import androidx.compose.foundation.layout.Row
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Surface
import androidx.compose.material3.toShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAccents
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStat
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStatGrid
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
import com.kylecorry.trail_sense.tools.weather.ui.SkyEmptyState
import com.kylecorry.trail_sense.tools.weather.ui.SkyValueRow
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.plugins.domain.PluginResourceServiceDetails
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The debug-only Experimentation tool: the connected plugins, one row each (name, package and version), tap a row to
 * see the features its resource service offers. A quiet loading indicator until the plugins are read.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ExperimentationContent() {
    val context = LocalContext.current
    val pluginSubsystem = remember { PluginSubsystem.getInstance(context) }
    var plugins by remember { mutableStateOf<List<PluginResourceServiceDetails>?>(null) }
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    LaunchedEffect(pluginSubsystem) {
        plugins = withContext(Dispatchers.IO) {
            pluginSubsystem.getPluginResourceServiceDetails()
        }
    }

    val loaded = plugins
    val pluginsTitle = stringResource(R.string.plugins)

    TrailSensePage(
        title = "Experimentation",
        onBack = { dispatcher?.onBackPressed() },
        backLabel = stringResource(R.string.back)
    ) {
        when {
            loaded == null -> item(key = "loading") {
                LoadingIndicator(modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp))
            }

            loaded.isEmpty() -> {
                item(key = "empty") {
                    // The kit's layered mark: one clear message, no placeholder rows pretending to be plugins.
                    TrailSenseEmptyState(
                        icon = Icons.Rounded.Extension,
                        title = stringResource(R.string.experimentation_no_plugins),
                        description = stringResource(R.string.sky_experimentation_empty),
                        modifier = Modifier.heightIn(min = 400.dp)
                    )
                }
                // Dashed outlines where installed plugins will sit, so the page previews what it is for.
                item(key = "slots") {
                    Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)) {
                        repeat(2) { ExperimentationDashedSlot() }
                    }
                }
            }

            else -> {
                item(key = "summary") {
                    TrailSenseStatGrid(
                        stats = listOf(
                            TrailSenseStat(
                                label = pluginsTitle,
                                value = loaded.size.toString(),
                                tone = TrailSenseTone.Primary,
                                icon = Icons.Rounded.Extension
                            ),
                            TrailSenseStat(
                                label = stringResource(R.string.sky_experimentation_features),
                                value = loaded.sumOf { it.features.weather.size + it.features.mapLayers.size }.toString(),
                                tone = TrailSenseTone.Tertiary,
                                icon = TrailIcons.Experimental
                            )
                        ),
                        columns = 2
                    )
                }
                trailSenseSection(key = "plugins", title = pluginsTitle) {
                    loaded.forEach { plugin ->
                        row { position -> ExperimentationPluginRow(plugin, position) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExperimentationPluginRow(
    plugin: PluginResourceServiceDetails,
    position: TrailSenseGroupPosition
) {
    var expanded by rememberSaveable(plugin.packageId) { mutableStateOf(false) }
    Column {
        SkyValueRow(
            title = plugin.name,
            subtitle = listOfNotNull(plugin.packageId, plugin.version).joinToString(" · "),
            icon = Icons.Rounded.Extension,
            accent = TrailSenseAccents.Lavender,
            badgeShape = MaterialShapes.Clover4Leaf.toShape(),
            position = if (expanded) position.copy(isLast = false) else position,
            onClick = { expanded = !expanded }
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec())
        ) {
            TrailSenseGroupCard(
                position = position.copy(isFirst = false),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TrailSenseSpacing.xxs)
            ) {
                Text(
                    text = plugin.features.toString(),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** A ghost row: where a plugin will sit once one is installed. */
@Composable
private fun ExperimentationEmptySlot(position: TrailSenseGroupPosition, alpha: Float) {
    Surface(
        shape = trailSenseGroupShape(position),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = alpha),
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Row(
            Modifier.padding(TrailSenseSpacing.lg),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialShapes.Clover4Leaf.toShape(),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha),
                modifier = Modifier.size(44.dp)
            ) {}
            androidx.compose.foundation.layout.Spacer(Modifier.size(TrailSenseSpacing.lg))
            Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha),
                    modifier = Modifier.size(width = 140.dp, height = 12.dp)
                ) {}
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha),
                    modifier = Modifier.size(width = 200.dp, height = 10.dp)
                ) {}
            }
        }
    }
}

/** An empty plugin slot: a dashed outline in the tertiary with a puzzle-piece badge. */
@Composable
private fun ExperimentationDashedSlot() {
    val outline = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .drawBehind {
                drawRoundRect(
                    color = outline,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
                    )
                )
            }
            .padding(TrailSenseSpacing.lg),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge(
            icon = Icons.Rounded.Extension,
            shape = MaterialShapes.Clover4Leaf.toShape(),
            size = 40.dp,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(TrailSenseSpacing.lg))
        Text(
            stringResource(R.string.sky_experimentation_slot),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
