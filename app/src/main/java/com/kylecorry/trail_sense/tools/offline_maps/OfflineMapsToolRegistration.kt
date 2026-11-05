package com.kylecorry.trail_sense.tools.offline_maps

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import android.os.Bundle
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.extensions.findNavigator
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerAttribution
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceType
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerType
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import com.kylecorry.trail_sense.tools.offline_maps.map_layers.PhotoMapTileSource
import com.kylecorry.trail_sense.tools.offline_maps.map_layers.TrailMapsTileSource
import com.kylecorry.trail_sense.tools.offline_maps.quickactions.QuickActionOpenPhotoMap
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolBroadcast
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIntentHandler
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object OfflineMapsToolRegistration : ToolRegistration {

    private val mapRefreshBroadcasts = listOf(
        BROADCAST_OFFLINE_MAP_CHANGED,
        BROADCAST_OFFLINE_MAP_ADDED,
        BROADCAST_OFFLINE_MAP_DELETED
    )

    private val importMapIntentHandler = ToolIntentHandler { activity, intent ->
        val validTypes = listOf("image/", "application/pdf")
        if (!validTypes.any { intent.type?.startsWith(it) == true }) {
            return@ToolIntentHandler false
        }

        val intentUri = intent.clipData?.getItemAt(0)?.uri ?: return@ToolIntentHandler false
        activity.findNavigator()?.navigate(Route.OfflineMapList(importUri = intentUri.toString()))
        true
    }

    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.OFFLINE_MAPS,
            context.getString(R.string.offline_maps),
            TrailIcons.PhotoMaps,
            Route.OfflineMapList(),
            ToolCategory.Location,
            context.getString(R.string.offline_maps_summary),
            guideId = R.raw.guide_tool_offline_maps,
            settingsRoute = Route.PhotoMapSettings,
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_OPEN_PHOTO_MAP,
                    context.getString(R.string.open_photo_map),
                    ::QuickActionOpenPhotoMap
                )
            ),
            additionalRoutes = listOf(
                Route.PhotoMaps::class,
                Route.VectorMapView::class
            ),
            diagnostics = listOf(
                ToolDiagnosticFactory.gps(context),
                ToolDiagnosticFactory.camera(context),
                *ToolDiagnosticFactory.compass(context)
            ).distinctBy { it.id },
            intentHandlers = listOf(importMapIntentHandler),
            singletons = listOf(
                MapService::getInstance
            ),
            broadcasts = listOf(
                ToolBroadcast(BROADCAST_OFFLINE_MAP_CHANGED, "Offline map changed"),
                ToolBroadcast(BROADCAST_OFFLINE_MAP_ADDED, "Offline map added"),
                ToolBroadcast(BROADCAST_OFFLINE_MAP_DELETED, "Offline map deleted"),
            ),
            mapLayers = listOf(
                MapLayerDefinition(
                    PhotoMapTileSource.SOURCE_ID,
                    context.getString(R.string.photo_maps),
                    layerType = MapLayerType.Tile,
                    description = context.getString(R.string.map_layer_photo_maps_description),
                    preferences = listOf(
                        MapLayerPreference(
                            id = PhotoMapTileSource.LOAD_PDFS,
                            title = context.getString(R.string.load_pdf_tiles),
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = PhotoMapTileSource.DEFAULT_LOAD_PDFS,
                            summary = context.getString(R.string.load_pdf_tiles_summary)
                        )
                    ),
                    tileSource = ::PhotoMapTileSource,
                    minZoomLevel = 4,
                    refreshBroadcasts = mapRefreshBroadcasts,
                    cacheKeys = listOf(
                        PhotoMapTileSource.LOAD_PDFS
                    )
                ),
                MapLayerDefinition(
                    TrailMapsTileSource.SOURCE_ID,
                    context.getString(R.string.trail_maps),
                    layerType = MapLayerType.Tile,
                    description = context.getString(R.string.map_layer_trail_maps_description),
                    preferences = listOf(
                        MapLayerPreference(
                            id = "offline_maps",
                            title = context.getString(R.string.manage_maps),
                            type = MapLayerPreferenceType.Label,
                            routeOnClick = Route.OfflineMapList()
                        )
                    ),
                    tileSource = ::TrailMapsTileSource,
                    refreshBroadcasts = mapRefreshBroadcasts,
                    attributionLoader = {
                        val attributions = getAppService<MapService>()
                            .getAllVectorMaps()
                            .filter { it.visible }
                            .mapNotNull { it.attribution?.trim()?.takeIf { attribution -> attribution.isNotBlank() } }
                            .distinct()

                        if (attributions.isEmpty()) {
                            null
                        } else {
                            MapLayerAttribution(
                                attributions.joinToString("\n"),
                                alwaysShow = true
                            )
                        }
                    }
                )
            )
        )
    }

    const val PHOTO_MAPS_ID = "photo_maps"
    const val BROADCAST_OFFLINE_MAP_CHANGED = "offline-maps-broadcast-offline-map-changed"
    const val BROADCAST_OFFLINE_MAP_ADDED = "offline-maps-broadcast-offline-map-added"
    const val BROADCAST_OFFLINE_MAP_DELETED = "offline-maps-broadcast-offline-map-deleted"
    const val BROADCAST_PARAM_OFFLINE_MAP_ID = "offline-map-id"
    const val BROADCAST_PARAM_OFFLINE_MAP_TYPE = "offline-map-type"
}
