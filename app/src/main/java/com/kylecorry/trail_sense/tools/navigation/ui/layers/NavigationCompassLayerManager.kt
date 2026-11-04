package com.kylecorry.trail_sense.tools.navigation.ui.layers

import android.content.Context
import android.view.View
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceRepo
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayersWithPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration

class NavigationCompassLayerManager {
    private val repo = DependencyRegistry.get<MapLayerPreferenceRepo>()

    var key = 0
        private set

    private var isRunning = false
    private val runningLock = Any()

    fun resume(context: Context, view: IMapView, host: TrailSenseDestinationHost) {
        synchronized(runningLock) {
            isRunning = true
        }
        host.inBackground {
            view.setLayersWithPreferences(
                NavigationToolRegistration.MAP_ID,
                // On a compass the centre is always you, so the "my location" arrow is noise there.
                repo.getActiveLayerIds(NavigationToolRegistration.MAP_ID)
                    .filterNot {
                        // ...and the line to the destination: the dial's rim marker already points at it.
                        it == com.kylecorry.trail_sense.tools.map.map_layers.MyLocationGeoJsonSource.SOURCE_ID ||
                            it == com.kylecorry.trail_sense.tools.navigation.map_layers.NavigationGeoJsonSource.SOURCE_ID
                    }
            )

            key += 1

            synchronized(runningLock) {
                if (isRunning) {
                    view.start()

                    if (view is View) {
                        view.invalidate()
                    }
                }
            }
        }
    }

    fun pause(view: IMapView) {
        synchronized(runningLock) {
            isRunning = false
            view.stop()
        }
    }
}
