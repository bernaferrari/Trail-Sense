package com.kylecorry.trail_sense.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.plugins.domain.Plugin
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import kotlinx.coroutines.launch

@Composable
fun PluginsSettingsToolScreen(host: TrailSenseDestinationHost) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val pluginSubsystem = remember { PluginSubsystem.getInstance(host.requireContext()) }
    var plugins by remember { mutableStateOf<PluginSettingsState?>(null) }

    fun refresh() {
        scope.launch {
            plugins = loadPlugins(pluginSubsystem)
        }
    }

    LaunchedEffect(Unit) {
        plugins = loadPlugins(pluginSubsystem)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val connectedTitle = stringResource(R.string.connected_plugins)
    val availableTitle = stringResource(R.string.available_plugins)

    SettingsList(host, title = stringResource(R.string.plugins)) {
        // Nothing until the first load finishes, so "None" never flashes over plugins that are there.
        val loaded = plugins ?: return@SettingsList
        trailSenseSection("connected", title = connectedTitle) {
            if (loaded.connected.isEmpty()) {
                row { position ->
                    SettingsInfoRow(label = stringResource(R.string.none), position = position)
                }
            }
            loaded.connected.forEach { plugin ->
                row { position ->
                    SettingsActionRow(
                        label = plugin.name,
                        summary = plugin.packageId,
                        onClick = { host.openPluginDetails(plugin) },
                        position = position
                    )
                }
            }
        }
        trailSenseSection("available", title = availableTitle) {
            if (loaded.available.isEmpty()) {
                row { position ->
                    SettingsInfoRow(label = stringResource(R.string.none), position = position)
                }
            }
            loaded.available.forEach { plugin ->
                row { position ->
                    SettingsActionRow(
                        label = plugin.name,
                        summary = plugin.packageId,
                        onClick = { host.confirmConnect(pluginSubsystem, plugin, ::refresh) },
                        position = position
                    )
                }
            }
        }
    }
}

private suspend fun loadPlugins(pluginSubsystem: PluginSubsystem): PluginSettingsState {
    return PluginSettingsState(
        connected = pluginSubsystem.getConnectedPlugins().sortedPlugins(),
        available = pluginSubsystem.getUnconnectedPlugins().sortedPlugins()
    )
}

private fun TrailSenseDestinationHost.confirmConnect(
    pluginSubsystem: PluginSubsystem,
    plugin: Plugin,
    onConnected: () -> Unit
) {
    Alerts.dialog(
        requireContext(),
        getString(R.string.plugin_connect_title),
        getString(R.string.plugin_connect_message, plugin.name)
    ) { cancelled ->
        if (!cancelled) {
            lifecycleScope.launch {
                pluginSubsystem.connect(plugin)
                onConnected()
            }
        }
    }
}

private fun TrailSenseDestinationHost.openPluginDetails(plugin: Plugin) {
    findNavigator().navigate(Route.PluginDetails(packageId = plugin.packageId))
}

private fun List<Plugin>.sortedPlugins(): List<Plugin> {
    return sortedWith(
        compareBy<Plugin, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
            .thenBy { it.packageId }
    )
}

private data class PluginSettingsState(
    val connected: List<Plugin>,
    val available: List<Plugin>
)
