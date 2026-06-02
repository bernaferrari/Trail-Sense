package com.kylecorry.trail_sense.settings.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.plugins.domain.Plugin
import com.kylecorry.trail_sense.plugins.domain.PluginResourceServiceDetails
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerType
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import kotlinx.coroutines.launch

class PluginDetailsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    PluginDetailsScreen(
                        packageId = arguments?.getString("package_id").orEmpty(),
                        pluginSubsystem = PluginSubsystem.getInstance(requireContext()),
                        onNavigateUp = {
                            tryOrNothing {
                                findNavController().navigateUp()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PluginDetailsScreen(
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = plugin?.name ?: stringResource(R.string.plugins),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "$packageId ($version)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (details == null || isUnavailable) {
                    Text(
                        text = if (details == null) {
                            stringResource(R.string.loading)
                        } else {
                            stringResource(R.string.plugin_unavailable)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = stringResource(R.string.map_layers),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    mapLayers.forEach {
                        MapLayerRow(it)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { launchIntent?.let { context.startActivity(it) } },
                            enabled = !isUnavailable && launchIntent != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.open))
                        }
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:$packageId")
                                    }
                                )
                            },
                            enabled = !isUnavailable,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.app_info))
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    pluginSubsystem.reloadRegistration(packageId)
                                    reloadKey += 1
                                    Alerts.toast(context, context.getString(R.string.done))
                                }
                            },
                            enabled = !isUnavailable,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.reload))
                        }
                        OutlinedButton(
                            onClick = {
                                val currentPlugin = plugin ?: return@OutlinedButton
                                Alerts.dialog(
                                    context,
                                    context.getString(R.string.plugin_disconnect_title),
                                    context.getString(
                                        R.string.plugin_disconnect_message,
                                        currentPlugin.name
                                    )
                                ) { cancelled ->
                                    if (!cancelled) {
                                        scope.launch {
                                            pluginSubsystem.disconnect(currentPlugin)
                                            onNavigateUp()
                                        }
                                    }
                                }
                            },
                            enabled = !isUnavailable,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.disconnect))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapLayerRow(layer: MapLayerDetails) {
    Column {
        Text(
            text = layer.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium
        )
        layer.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        layer.type?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
