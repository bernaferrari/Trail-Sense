package com.kylecorry.trail_sense.tools.experimentation

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

            loaded.isEmpty() -> item(key = "empty") {
                TrailSenseEmptyState(
                    icon = Icons.Rounded.Extension,
                    title = stringResource(R.string.experimentation_no_plugins),
                    modifier = Modifier.heightIn(max = 420.dp)
                )
            }

            else -> trailSenseSection(key = "plugins", title = pluginsTitle) {
                loaded.forEach { plugin ->
                    row { position -> ExperimentationPluginRow(plugin, position) }
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
        TrailSenseListItem(
            title = plugin.name,
            subtitle = listOfNotNull(plugin.packageId, plugin.version).joinToString(" · "),
            icon = rememberVectorPainter(TrailIcons.Experimental),
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
