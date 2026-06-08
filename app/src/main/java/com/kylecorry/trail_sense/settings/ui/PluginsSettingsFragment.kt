package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.plugins.domain.Plugin
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlinx.coroutines.launch

class PluginsSettingsFragment : TrailSenseComposeFragment() {

    private val pluginSubsystem = getAppService<PluginSubsystem>()

    @Composable
    override fun FragmentContent() {
        PluginsSettingsScreen()
    }

    @Composable
    private fun PluginsSettingsScreen() {
        val lifecycleOwner = LocalLifecycleOwner.current
        val scope = rememberCoroutineScope()
        var plugins by remember { mutableStateOf<PluginSettingsState?>(null) }

        fun refresh() {
            scope.launch {
                plugins = loadPlugins()
            }
        }

        LaunchedEffect(Unit) {
            plugins = loadPlugins()
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

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = TrailSenseSpacing.lg,
                    end = TrailSenseSpacing.lg,
                    top = TrailSenseSpacing.lg,
                    bottom = TrailSenseSpacing.xxxl
                )
            ) {
                item("connected_header") {
                    TrailSenseSectionHeader(title = stringResource(R.string.connected_plugins))
                }
                val connected = plugins?.connected.orEmpty()
                if (connected.isEmpty()) {
                    item("connected_none") {
                        SettingsActionRow(
                            label = stringResource(R.string.none),
                            enabled = false,
                            onClick = {},
                            trailing = {},
                            modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                        )
                    }
                } else {
                    items(connected, key = { "connected_${it.packageId}" }) { plugin ->
                        SettingsActionRow(
                            label = plugin.name,
                            summary = plugin.packageId,
                            onClick = { openDetails(plugin) },
                            modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                        )
                    }
                }

                item("available_header") {
                    TrailSenseSectionHeader(
                        title = stringResource(R.string.available_plugins),
                        modifier = Modifier.padding(top = TrailSenseSpacing.md)
                    )
                }
                val available = plugins?.available.orEmpty()
                if (available.isEmpty()) {
                    item("available_none") {
                        SettingsActionRow(
                            label = stringResource(R.string.none),
                            enabled = false,
                            onClick = {},
                            trailing = {},
                            modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                        )
                    }
                } else {
                    items(available, key = { "available_${it.packageId}" }) { plugin ->
                        SettingsActionRow(
                            label = plugin.name,
                            summary = plugin.packageId,
                            onClick = { confirmConnect(plugin, ::refresh) },
                            modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadPlugins(): PluginSettingsState {
        return PluginSettingsState(
            connected = pluginSubsystem.getConnectedPlugins().sorted(),
            available = pluginSubsystem.getUnconnectedPlugins().sorted()
        )
    }

    private fun confirmConnect(plugin: Plugin, onConnected: () -> Unit) {
        Alerts.dialog(
            requireContext(),
            getString(R.string.plugin_connect_title),
            getString(R.string.plugin_connect_message, plugin.name)
        ) { cancelled ->
            if (!cancelled) {
                viewLifecycleOwner.lifecycleScope.launch {
                    pluginSubsystem.connect(plugin)
                    onConnected()
                }
            }
        }
    }

    private fun openDetails(plugin: Plugin) {
        findNavController().navigateWithAnimation(
            R.id.pluginDetailsFragment,
            Bundle().apply {
                putString("package_id", plugin.packageId)
            }
        )
    }

    private data class PluginSettingsState(
        val connected: List<Plugin>,
        val available: List<Plugin>
    )

    private fun List<Plugin>.sorted(): List<Plugin> {
        return sortedWith(
            compareBy<Plugin, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
                .thenBy { it.packageId }
        )
    }
}
