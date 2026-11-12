package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.text.RawText
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnostic
import kotlin.reflect.KClass

data class Tool(
    override val id: Long,
    override val name: String,
    override val icon: ImageVector,
    override val route: Route,
    override val category: ToolCategory,
    override val description: String? = null,
    override val guideId: RawText? = null,
    override val isExperimental: Boolean = false,
    override val settingsRoute: Route? = null,
    val quickActions: List<ToolQuickAction> = emptyList(),
    val widgets: List<ToolWidget> = emptyList(),
    override val additionalRoutes: List<KClass<out Route>> = emptyList(),
    val volumeActions: List<ToolVolumeAction> = emptyList(),
    val tiles: List<String> = emptyList(),
    val notificationChannels: List<ToolNotificationChannel> = emptyList(),
    val notificationChannelGroups: List<ToolNotificationChannelGroup> = emptyList(),
    val services: List<ToolService> = emptyList(),
    val diagnostics: List<ToolDiagnostic> = emptyList(),
    val intentHandlers: List<ToolIntentHandler> = emptyList(),
    val isAvailable: (context: Context) -> Boolean = { true },
    val broadcasts: List<ToolBroadcast> = emptyList(),
    val actions: List<ToolAction> = emptyList(),
    val initialize: (context: Context) -> Unit = {},
    val singletons: List<(Context) -> Any> = emptyList(),
    val mapLayers: List<MapLayerDefinition> = emptyList()
) : ToolDescriptor

