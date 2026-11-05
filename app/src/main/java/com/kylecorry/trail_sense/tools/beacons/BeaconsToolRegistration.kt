package com.kylecorry.trail_sense.tools.beacons

import com.kylecorry.trail_sense.shared.navigation.toRouteLocation

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import android.os.Build
import android.os.Bundle
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.findNavigator
import com.kylecorry.trail_sense.shared.extensions.getLongProperty
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconGeoJsonSource
import com.kylecorry.trail_sense.tools.beacons.quickactions.QuickActionPlaceBeacon
import com.kylecorry.trail_sense.tools.beacons.widgets.AppWidgetNearbyBeacons
import com.kylecorry.trail_sense.tools.beacons.widgets.NearbyBeaconsToolWidgetView
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration
import com.kylecorry.trail_sense.tools.sensors.SensorsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolBroadcast
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIntentHandler
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolQuickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolSummarySize
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolWidget
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnosticFactory

object BeaconsToolRegistration : ToolRegistration {

    private val geoIntentHandler =
        ToolIntentHandler { activity, intent ->
            val intentData = intent.data ?: return@ToolIntentHandler false
            if (intent.scheme != "geo") return@ToolIntentHandler false
            val geo = GeoUri.from(intentData) ?: return@ToolIntentHandler false

            activity.findNavigator()?.navigate(Route.BeaconList(initialLocation = geo.toRouteLocation()))
            true
        }

    private val openBeaconIntentHandler =
        ToolIntentHandler { activity, intent ->
            val beaconId = intent.getLongExtra("beacon_id", -1L)
            if (beaconId == -1L) return@ToolIntentHandler false
            activity.findNavigator()?.navigate(Route.BeaconDetails(beaconId))
            true
        }

    override fun getTool(context: Context): Tool {
        return Tool(
            Tools.BEACONS,
            context.getString(R.string.beacons),
            TrailIcons.Location,
            Route.BeaconList(),
            ToolCategory.Location,
            guideId = R.raw.guide_tool_beacons,
            settingsRoute = Route.BeaconSettings,
            quickActions = listOf(
                ToolQuickAction(
                    Tools.QUICK_ACTION_CREATE_BEACON,
                    context.getString(R.string.create_beacon),
                    ::QuickActionPlaceBeacon
                )
            ),
            additionalRoutes = listOf(
                Route.BeaconDetails::class,
                Route.PlaceBeacon::class
            ),
            diagnostics = listOf(
                ToolDiagnosticFactory.gps(context),
                *ToolDiagnosticFactory.altimeter(context),
                ToolDiagnosticFactory.camera(context),
                *ToolDiagnosticFactory.sightingCompass(context)
            ).distinctBy { it.id },
            intentHandlers = listOf(geoIntentHandler, openBeaconIntentHandler),
            broadcasts = listOf(
                ToolBroadcast(BROADCAST_BEACONS_CHANGED, "Beacons changed")
            ),
            singletons = listOf(
                { BeaconService(it) }
            ),
            widgets = listOf(
                ToolWidget(
                    WIDGET_NEARBY_BEACONS,
                    context.getString(R.string.nearby_beacons),
                    ToolSummarySize.Full,
                    NearbyBeaconsToolWidgetView(),
                    AppWidgetNearbyBeacons::class.java,
                    updateBroadcasts = listOf(
                        BROADCAST_BEACONS_CHANGED,
                        SensorsToolRegistration.BROADCAST_LOCATION_CHANGED
                    ),
                    usesLocation = true,
                    canPlaceInApp = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                )
            ),
            mapLayers = listOf(
                MapLayerDefinition(
                    MAP_LAYER_BEACONS,
                    context.getString(R.string.beacons),
                    description = context.getString(R.string.map_layer_beacons_description),
                    openFeature = { feature, _, appNavigator ->
                        val beaconId =
                            feature.getLongProperty(BeaconGeoJsonSource.PROPERTY_BEACON_ID)
                        beaconId?.let { appNavigator.navigate(Route.BeaconDetails(it)) }
                    },
                    geoJsonSource = ::BeaconGeoJsonSource,
                    refreshBroadcasts = listOf(
                        NavigationToolRegistration.BROADCAST_DESTINATION_CHANGED
                    )
                )
            )
        )
    }

    const val BROADCAST_BEACONS_CHANGED = "beacons-broadcast-beacons-changed"
    const val WIDGET_NEARBY_BEACONS = "beacons-widget-nearby-beacons"
    const val MAP_LAYER_BEACONS = "beacon"
}
