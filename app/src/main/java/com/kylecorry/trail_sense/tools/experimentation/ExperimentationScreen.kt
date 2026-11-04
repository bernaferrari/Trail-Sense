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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.plugins.domain.PluginResourceServiceDetails
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
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

    // One question: which plugins are connected? The count answers it; each plugin is one row.
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
                    // One message and one way out: the plugin settings, which list the plugins that can be connected.
                    TrailSenseEmptyState(
                        icon = Icons.Rounded.Extension,
                        title = stringResource(R.string.experimentation_no_plugins),
                        // Say what a plugin is, in plain words: a separate app that adds data to Trail Sense.
                        description = stringResource(R.string.experimentation_what_is_a_plugin),
                        actionLabel = stringResource(R.string.experimentation_find_plugins),
                        onAction = {
                            context.findComponentActivity()?.findNavController()
                                ?.navigate(R.id.pluginsSettingsFragment)
                        },
                        // The same optical centre as the other empty screens.
                        modifier = Modifier.fillParentMaxHeight(0.9f)
                    )
                }
            }

            else -> {
                item(key = "summary") {
                    val weather = loaded.sumOf { it.features.weather.size }
                    val layers = loaded.sumOf { it.features.mapLayers.size }
                    Column(Modifier.padding(horizontal = TrailSenseSpacing.lg)) {
                        Text(
                            pluralStringResource(R.plurals.experimentation_plugin_count, loaded.size, loaded.size),
                            style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        // What they add, in plain words, instead of a bare "features" count.
                        Text(
                            addsText(weather, layers),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = TrailSenseSpacing.xs)
                        )
                    }
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

/** "Adds 2 weather sources and 1 map layer", or that it adds nothing yet. */
@Composable
private fun addsText(weather: Int, layers: Int): String {
    val parts = listOfNotNull(
        pluralStringResource(R.plurals.experimentation_weather_count, weather, weather).takeIf { weather > 0 },
        pluralStringResource(R.plurals.experimentation_layer_count, layers, layers).takeIf { layers > 0 }
    )
    return if (parts.isEmpty()) {
        stringResource(R.string.experimentation_adds_nothing)
    } else {
        stringResource(R.string.experimentation_adds, parts.joinToString(stringResource(R.string.experimentation_and)))
    }
}

/**
 * One plugin: its name, what it adds (and its version) underneath. Tapping it lists each feature by name, with the
 * package id last for whoever needs it.
 */
@Composable
private fun ExperimentationPluginRow(
    plugin: PluginResourceServiceDetails,
    position: TrailSenseGroupPosition
) {
    var expanded by rememberSaveable(plugin.packageId) { mutableStateOf(false) }
    val adds = addsText(plugin.features.weather.size, plugin.features.mapLayers.size)
    Column {
        TrailSenseListItem(
            title = plugin.name,
            subtitle = listOfNotNull(adds, plugin.version).joinToString(" · "),
            icon = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Rounded.Extension),
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
                Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)) {
                    plugin.features.mapLayers.forEach { layer ->
                        Text(
                            stringResource(R.string.experimentation_map_layer, layer.name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    plugin.features.weather.forEach { feature ->
                        Text(
                            stringResource(R.string.experimentation_weather_feature, feature),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        plugin.packageId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private tailrec fun android.content.Context.findComponentActivity(): androidx.activity.ComponentActivity? =
    when (this) {
        is androidx.activity.ComponentActivity -> this
        is android.content.ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
