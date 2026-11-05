package com.kylecorry.trail_sense.tools.tools.infrastructure

import androidx.compose.ui.graphics.vector.ImageVector
import android.content.Context
import kotlin.reflect.KClass
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.data.Identifiable
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnostic

data class Tool(
    override val id: Long,
    val name: String,
    val icon: ImageVector,
    val route: Route,
    val category: ToolCategory,
    val description: String? = null,
    val guideId: Int? = null,
    val isExperimental: Boolean = false,
    val settingsRoute: Route? = null,
    val quickActions: List<ToolQuickAction> = emptyList(),
    val widgets: List<ToolWidget> = emptyList(),
    /** Other screens that belong to this tool (its editors and details), so its tab stays lit on them. */
    val additionalRoutes: List<KClass<out Route>> = emptyList(),
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
) : Identifiable {
    fun isOpen(current: Route?): Boolean {
        current ?: return false
        return route::class == current::class || additionalRoutes.any { it.isInstance(current) }
    }
}

