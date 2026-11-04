package com.kylecorry.trail_sense.tools.offline_maps.quickactions

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.os.Bundle
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.extensions.withCancelableLoading
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.selection.ActiveMapSelector
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import kotlinx.coroutines.launch

class QuickActionOpenPhotoMap(button: QuickActionButtonView, host: TrailSenseDestinationHost) : QuickActionButton(
    button, host
) {
    private val mapService = MapService.getInstance(host.requireContext())
    private val navigator = Navigator.getInstance(host.requireContext())
    private val selector = ActiveMapSelector()

    override fun onCreate() {
        super.onCreate()
        setIcon(TrailIcons.PhotoMaps)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.findNavController().navigate(R.id.mapListFragment)
        return true
    }

    override fun onClick() {
        super.onClick()
        host.inBackground {
            var wasSuccessful = false
            var id = 0L
            val job = launch {
                id = getActiveMap()?.id ?: 0
                wasSuccessful = true
            }


            Alerts.withCancelableLoading(
                host.requireContext(),
                context.getString(R.string.loading),
                onCancel = { job.cancel() }) {
                job.join()
                if (wasSuccessful) {
                    if (id != 0L) {
                        host.findNavController()
                            .navigate(
                                R.id.photoMapsFragment,
                                Bundle().apply {
                                    putLong("mapId", id)
                                    putBoolean("autoLockLocation", true)
                                }
                            )
                    } else {
                        host.findNavController().navigate(R.id.mapListFragment)
                    }
                }
            }
        }
    }

    private suspend fun getActiveMap(): PhotoMap? {
        val sensors = SensorSubsystem.getInstance(host.requireContext())
        val location = sensors.getLocation()

        val destination = (navigator.getDestination() as? Destination.Beacon)?.beacon?.coordinate
        val maps = mapService.getAllPhotoMaps()
        return selector.getActiveMap(maps, location, destination)
    }

}
