@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.settings.ui

import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.plugins.domain.Plugin
import com.kylecorry.trail_sense.plugins.domain.PluginResourceServiceDetails
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerType
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseLoadingIndicator
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import kotlinx.coroutines.launch

@Composable
internal fun PluginDetailsScreen(
    packageId: String,
    pluginSubsystem: PluginSubsystem,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reloadKey by remember { mutableIntStateOf(0) }
    var details by remember { mutableStateOf<PluginDetails?>(null) }

    LaunchedEffect(packageId, reloadKey) {
        details = null
        details = loadDetails(pluginSubsystem, packageId)
    }

    val plugin = details?.plugin
    val resourceDetails = details?.resourceServiceDetails
    val isLoading = details == null
    val isUnavailable = details?.plugin == null || details?.resourceServiceDetails == null
    val launchIntent = remember(packageId, isUnavailable) {
        if (isUnavailable) {
            null
        } else {
            context.packageManager.getLaunchIntentForPackage(packageId)
        }
    }
    val version = plugin?.version ?: resourceDetails?.version ?: stringResource(R.string.unknown)
    val mapLayers = getMapLayers(
        mapLayers = resourceDetails?.features?.mapLayers,
        pluginName = resourceDetails?.name ?: plugin?.name
    )
    val mapLayersTitle = stringResource(R.string.map_layers)
    val unavailableText = stringResource(R.string.plugin_unavailable)
    val appsIcon = rememberVectorPainter(TrailIcons.Apps)

    TrailSensePage(
        title = plugin?.name ?: stringResource(R.string.plugins),
        onBack = onNavigateUp,
        backLabel = stringResource(R.string.back)
    ) {
        item("header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(TrailSenseShape.iconContainer),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(appsIcon, contentDescription = null, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(Modifier.width(TrailSenseSpacing.lg))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = packageId,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = version,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (isLoading) {
            item("loading") {
                TrailSenseLoadingIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }
            return@TrailSensePage
        }

        if (isUnavailable) {
            trailSenseSection("unavailable") {
                row { position ->
                    TrailSenseGroupCard(position = position) {
                        Text(
                            text = unavailableText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        trailSenseSection("map_layers", title = mapLayersTitle) {
            mapLayers.forEach { layer ->
                row { position ->
                    SettingsInfoRow(
                        label = layer.name,
                        summary = listOfNotNull(layer.description, layer.type)
                            .joinToString(" · ")
                            .takeIf { it.isNotEmpty() },
                        position = position
                    )
                }
            }
        }

        if (!isUnavailable) {
            trailSenseSection("actions") {
                val intent = launchIntent
                if (intent != null) {
                    row { position ->
                        SettingsActionRow(
                            label = stringResource(R.string.open),
                            onClick = { context.startActivity(intent) },
                            position = position
                        )
                    }
                }
                row { position ->
                    SettingsActionRow(
                        label = stringResource(R.string.app_info),
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:$packageId")
                                }
                            )
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsActionRow(
                        label = stringResource(R.string.reload),
                        onClick = {
                            scope.launch {
                                pluginSubsystem.reloadRegistration(packageId)
                                reloadKey += 1
                                Alerts.toast(context, context.getString(R.string.done))
                            }
                        },
                        position = position
                    )
                }
            }

            trailSenseSection("disconnect") {
                row { position ->
                    TrailSenseListItem(
                        title = stringResource(R.string.disconnect),
                        onClick = {
                            val currentPlugin = plugin ?: return@TrailSenseListItem
                            Alerts.dialog(
                                context,
                                context.getString(R.string.plugin_disconnect_title),
                                context.getString(R.string.plugin_disconnect_message, currentPlugin.name)
                            ) { cancelled ->
                                if (!cancelled) {
                                    scope.launch {
                                        pluginSubsystem.disconnect(currentPlugin)
                                        onNavigateUp()
                                    }
                                }
                            }
                        },
                        position = position,
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                }
            }
        }
    }
}

private suspend fun loadDetails(
    pluginSubsystem: PluginSubsystem,
    packageId: String
): PluginDetails {
    val plugin = pluginSubsystem.getConnectedPlugins()
        .firstOrNull { it.packageId == packageId }
    return PluginDetails(
        plugin,
        plugin?.let { pluginSubsystem.getPluginResourceServiceDetails(packageId) }
    )
}

@Composable
private fun getMapLayers(
    mapLayers: List<MapLayerDefinition>?,
    pluginName: String?
): List<MapLayerDetails> {
    val layers = mapLayers.orEmpty()
    return if (layers.isEmpty()) {
        listOf(MapLayerDetails(stringResource(R.string.none), null, null))
    } else {
        layers.map {
            MapLayerDetails(
                it.name.withoutPluginPrefix(pluginName),
                it.description.withoutPluginDescriptionPrefix(pluginName),
                it.layerType.getName()
            )
        }
    }
}

@Composable
private fun MapLayerType.getName(): String {
    return when (this) {
        MapLayerType.Tile -> stringResource(R.string.tile_layer)
        MapLayerType.Feature -> stringResource(R.string.feature_layer)
        MapLayerType.Overlay -> stringResource(R.string.overlay_layer)
    }
}

private fun String.withoutPluginPrefix(pluginName: String?): String {
    val prefix = "${pluginName ?: return this}:"
    return removePrefix(prefix).trimStart()
}

@Composable
private fun String?.withoutPluginDescriptionPrefix(pluginName: String?): String? {
    val prefix = stringResource(R.string.plugin_name, pluginName ?: return this)
    return this
        ?.removePrefix(prefix)
        ?.trimStart()
        ?.takeIf { it.isNotEmpty() }
}

private data class PluginDetails(
    val plugin: Plugin?,
    val resourceServiceDetails: PluginResourceServiceDetails?
)

private data class MapLayerDetails(
    val name: String,
    val description: String?,
    val type: String?
)
