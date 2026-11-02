package com.kylecorry.trail_sense.tools.experimentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@androidx.compose.runtime.Composable
internal fun ExperimentationContent() {
    val context = LocalContext.current
    val pluginSubsystem = remember { PluginSubsystem.getInstance(context) }
    var pluginsText by remember { mutableStateOf("") }

    LaunchedEffect(pluginSubsystem) {
        val plugins = withContext(Dispatchers.IO) {
            pluginSubsystem.getPluginResourceServiceDetails()
        }
        pluginsText = plugins.joinToString("\n\n")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(TrailSenseSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
    ) {
        Text(
            text = pluginsText.ifBlank { "No plugins" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
