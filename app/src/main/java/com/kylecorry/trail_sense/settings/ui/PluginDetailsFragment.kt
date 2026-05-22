package com.kylecorry.trail_sense.settings.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.plugins.domain.Plugin
import com.kylecorry.trail_sense.plugins.domain.PluginResourceServiceDetails
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerType
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChip
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import kotlinx.coroutines.launch

@Composable
fun Fragment.PluginDetailsContent(modifier: Modifier = Modifier) {
    val context = requireContext()
    val navController = findNavController()
    val scope = rememberCoroutineScope()

    val pluginSubsystem = remember { PluginSubsystem.getInstance(context) }
    val packageId = remember {
        arguments?.getString("package_id").orEmpty()
    }

    var reloadKey by remember { mutableIntStateOf(0) }
    var details by remember { mutableStateOf<PluginDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(packageId, reloadKey) {
        isLoading = true
        details = loadDetails(pluginSubsystem, packageId)
        isLoading = false
    }

    val plugin = details?.plugin
    val resourceDetails = details?.resourceServiceDetails
    val isUnavailable = plugin == null || resourceDetails == null

    val launchIntent = remember(packageId, isUnavailable) {
        if (isUnavailable) null
        else context.packageManager.getLaunchIntentForPackage(packageId)
    }

    val mapLayers = resourceDetails?.features?.mapLayers.orEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TrailSenseCard {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = plugin?.name ?: getString(R.string.plugins),
                    style = MaterialTheme.typography.titleLarge
                )
                val version = plugin?.version ?: resourceDetails?.version ?: getString(R.string.unknown)
                Text(
                    text = "$packageId ($version)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isLoading) {
            Text(getString(R.string.loading))
        } else if (isUnavailable) {
            TrailSenseCard {
                Text(
                    getString(R.string.plugin_unavailable),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (mapLayers.isNotEmpty()) {
            Text(
                "Map Layers",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(mapLayers) { layer ->
                    TrailSenseCard {
                        Column(Modifier.padding(12.dp)) {
                            Row {
                                Text(
                                    layer.name.withoutPluginPrefix(plugin?.name),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                TrailSenseChip(
                                    text = layer.layerType.getName(),
                                    selected = false,
                                    onClick = { }
                                )
                            }
                            layer.description.withoutPluginDescriptionPrefix(plugin?.name)?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (launchIntent != null) {
                TrailSenseButton(
                    text = getString(R.string.open),
                    onClick = { startActivity(launchIntent) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            TrailSenseButton(
                text = getString(R.string.app_info),
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageId")
                    }
                    startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUnavailable
            )

            TrailSenseButton(
                text = getString(R.string.reload),
                onClick = {
                    scope.launch {
                        pluginSubsystem.reloadRegistration(packageId)
                        reloadKey++
                        Alerts.toast(context, getString(R.string.done))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUnavailable
            )

            TrailSenseButton(
                text = getString(R.string.disconnect),
                onClick = {
                    plugin?.let { p ->
                        Alerts.dialog(
                            context,
                            getString(R.string.plugin_disconnect_title),
                            getString(R.string.plugin_disconnect_message, p.name)
                        ) { cancelled ->
                            if (!cancelled) {
                                scope.launch {
                                    pluginSubsystem.disconnect(p)
                                    try { navController.navigateUp() } catch (_: Exception) {}
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUnavailable
            )
        }
    }
}

class PluginDetailsFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        PluginDetailsContent()
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
        pluginSubsystem.getResourceServiceDetails(plugin)
    )
}

private fun String.withoutPluginPrefix(pluginName: String?): String {
    val prefix = "${pluginName ?: return this}:"
    return removePrefix(prefix).trimStart()
}

private fun String?.withoutPluginDescriptionPrefix(pluginName: String?): String? {
    return this?.takeIf { it.isNotBlank() }
}

private fun MapLayerType.getName(): String {
    return when (this) {
        MapLayerType.Tile -> "Tile Layer"
        MapLayerType.Feature -> "Feature Layer"
        MapLayerType.Overlay -> "Overlay Layer"
    }
}

private data class PluginDetails(
    val plugin: Plugin?,
    val resourceServiceDetails: PluginResourceServiceDetails?
)
        val (reloadKey, setReloadKey) = useState(0)
        val details = useBackgroundMemo(packageId, reloadKey, resetOnResume) {
            loadDetails(pluginSubsystem, packageId)
        }
        val isUnavailable = details?.plugin == null || details.resourceServiceDetails == null
        val plugin = details?.plugin
        val resourceDetails = details?.resourceServiceDetails
        val launchIntent = useMemo(packageId, isUnavailable) {
            if (isUnavailable) {
                null
            } else {
                requireContext().packageManager.getLaunchIntentForPackage(packageId)
            }
        }

        useEffect(
            titleView,
            unavailableView,
            mapLayersView,
            details
        ) {
            titleView.title.text = plugin?.name ?: getString(R.string.plugins)
            val version = plugin?.version ?: resourceDetails?.version ?: getString(R.string.unknown)
            titleView.subtitle.text = "$packageId ($version)"
            unavailableView.isVisible = isUnavailable
            unavailableView.text = if (details == null) {
                getString(R.string.loading)
            } else {
                getString(R.string.plugin_unavailable)
            }
            mapLayersView.setItems(
                getMapLayerItems(
                    resourceDetails?.features?.mapLayers,
                    resourceDetails?.name ?: plugin?.name
                )
            )
        }

        useEffect(
            openPluginButton,
            appInfoButton,
            reloadButton,
            disconnectButton,
            isUnavailable,
            launchIntent
        ) {
            openPluginButton.isVisible = launchIntent != null
            openPluginButton.isEnabled = !isUnavailable && launchIntent != null
            appInfoButton.isEnabled = !isUnavailable
            reloadButton.isEnabled = !isUnavailable
            disconnectButton.isEnabled = !isUnavailable
        }

        useEffect(openPluginButton, launchIntent) {
            openPluginButton.setOnClickListener {
                launchIntent?.let { startActivity(it) }
            }
        }

        useEffect(appInfoButton, packageId) {
            appInfoButton.setOnClickListener {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageId")
                }
                startActivity(intent)
            }
        }

        useEffect(reloadButton, pluginSubsystem, packageId, reloadKey) {
            reloadButton.setOnClickListener {
                lifecycleScope.launch {
                    pluginSubsystem.reloadRegistration(packageId)
                    setReloadKey(reloadKey + 1)
                    Alerts.toast(requireContext(), getString(R.string.done))
                }
            }
        }

        useEffect(disconnectButton, plugin, pluginSubsystem) {
            disconnectButton.setOnClickListener {
                plugin ?: return@setOnClickListener
                Alerts.dialog(
                    requireContext(),
                    getString(R.string.plugin_disconnect_title),
                    getString(R.string.plugin_disconnect_message, plugin.name)
                ) { cancelled ->
                    if (!cancelled) {
                        lifecycleScope.launch {
                            pluginSubsystem.disconnect(plugin)
                            tryOrNothing {
                                findNavController().navigateUp()
                            }
                        }
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

    private fun getMapLayerItems(
        mapLayers: List<MapLayerDefinition>?,
        pluginName: String?
    ): List<ListItem> {
        val layers = mapLayers.orEmpty()
        return if (layers.isEmpty()) {
            listOf(ListItem(-1, getString(R.string.none)))
        } else {
            layers.map {
                ListItem(
                    it.id.hashCode().toLong(),
                    it.name,
                    it.description.withoutPluginDescriptionPrefix(pluginName),
                    tags = listOf(
                        ListItemTag(
                            it.layerType.getName(),
                            null,
                            Resources.androidTextColorSecondary(requireContext())
                        )
                    )
                )
            }
        }
    }

    private fun MapLayerType.getName(): String {
        return when (this) {
            MapLayerType.Tile -> getString(R.string.tile_layer)
            MapLayerType.Feature -> getString(R.string.feature_layer)
            MapLayerType.Overlay -> getString(R.string.overlay_layer)
        }
    }

    private fun String?.withoutPluginDescriptionPrefix(pluginName: String?): String? {
        val prefix = getString(R.string.plugin_name, pluginName ?: return this)
        return this
            ?.removePrefix(prefix)
            ?.trimStart()
            ?.takeIf { it.isNotEmpty() }
    }

    private data class PluginDetails(
        val plugin: Plugin?,
        val resourceServiceDetails: PluginResourceServiceDetails?
    )
}
