package com.kylecorry.trail_sense.tools.map.ui

import android.content.Context
import android.os.Bundle
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonPoint
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.extensions.getLayerId
import com.kylecorry.trail_sense.shared.extensions.getLongProperty
import com.kylecorry.trail_sense.shared.extensions.getName
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.sharing.ActionItem
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconGeoJsonSource
import com.kylecorry.trail_sense.tools.map.MapToolRegistration
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * What tapping a GeoJSON feature on a map does: select it, offer Navigate (and Open, when its layer can open
 * features) in the action sheet, then deselect it. The same flow as the shared GeoJsonFeatureClickHandler, over
 * [ScreenEffects] instead of the destination host; the map and photo map screens use it.
 */
internal class MapFeatureClickHandler(
    private val context: Context,
    private val navigator: TrailSenseNavigator,
    private val effects: ScreenEffects,
    private val scope: CoroutineScope
) {

    fun onFeatureClick(feature: GeoJsonFeature) {
        val layerId = feature.getLayerId()
        broadcastSelection(feature.id?.toString(), layerId)

        val formatter = DependencyRegistry.get<FormatService>()
        val location = getFeatureLocation(feature) ?: return
        val name = feature.getName()
        val title = name ?: context.getString(R.string.location)
        val layer = if (layerId != null) {
            Tools.getTools(context)
                .flatMap { it.mapLayers }
                .firstOrNull { it.id == layerId }
        } else {
            null
        }

        val navigate = ActionItem(context.getString(R.string.navigate), TrailIcons.Beacon) {
            val beaconId = feature.getLongProperty(BeaconGeoJsonSource.PROPERTY_BEACON_ID)
            val appNavigator = DependencyRegistry.get<Navigator>()
            if (beaconId != null) {
                appNavigator.navigateTo(beaconId)
            } else {
                appNavigator.navigateTo(location, name ?: "", BeaconOwner.Maps)
            }
        }
        val open = layer?.openFeature?.let { openFeature ->
            ActionItem(context.getString(R.string.open), TrailIcons.Open) {
                openFeature(feature, context, navigator)
            }
        }

        scope.launch {
            val chosen = effects.chooseAction(
                title,
                formatter.formatLocation(location),
                listOfNotNull(navigate, open)
            )
            chosen?.action?.invoke()
            // Broadcast deselection after the action
            broadcastSelection(null, layerId)
        }
    }

    private fun broadcastSelection(featureId: String?, layerId: String?) {
        val bundle = Bundle().apply {
            putString(MapToolRegistration.BROADCAST_PARAM_GEOJSON_FEATURE_ID, featureId)
            putString(MapToolRegistration.BROADCAST_PARAM_GEOJSON_LAYER_ID, layerId)
        }
        Tools.broadcast(MapToolRegistration.BROADCAST_GEOJSON_FEATURE_SELECTION_CHANGED, bundle)
    }

    private fun getFeatureLocation(feature: GeoJsonFeature): Coordinate? {
        val point = feature.geometry as? GeoJsonPoint ?: return null
        return point.point?.coordinate
    }
}
