package com.kylecorry.trail_sense.tools.beacons.quickactions

import android.os.Bundle
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.google.android.material.snackbar.Snackbar
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.sense.readAll
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.withCancelableLoading
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import java.time.ZonedDateTime
import kotlinx.coroutines.launch

class QuickActionPlaceBeacon(btn: QuickActionButtonView, host: TrailSenseDestinationHost) :
    QuickActionButton(btn, host) {

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.ic_location)
    }

    override fun onClick() {
        super.onClick()
        host.inBackground {

            var wasSuccessful = false
            var id = 0L
            val job = launch {
                val sensors = SensorService(host.requireContext())
                val gps = sensors.getGPS()
                val altimeter = sensors.getAltimeter(gps = gps)
                readAll(listOf(gps, altimeter))

                // Create a beacon
                val formatter = FormatService.getInstance(host.requireContext())
                val time = formatter.formatDateTime(ZonedDateTime.now())

                val prefs = UserPreferences(host.requireContext())
                val beaconService = BeaconService(host.requireContext())
                id = beaconService.add(
                    Beacon(
                        0,
                        time,
                        gps.location,
                        elevation = altimeter.altitude,
                        color = prefs.beacons.defaultBeaconColor.color,
                    )
                )

                wasSuccessful = true
            }


            Alerts.withCancelableLoading(
                host.requireContext(),
                context.getString(R.string.creating_beacon),
                onCancel = { job.cancel() }) {
                job.join()
                if (wasSuccessful) {
                    CustomUiUtils.snackbar(
                        host,
                        host.getString(R.string.beacon_created),
                        duration = Snackbar.LENGTH_LONG,
                        action = host.getString(R.string.view)
                    ) {
                        host.findNavController().navigateWithAnimation(
                            R.id.beaconDetailsFragment,
                            Bundle().apply {
                                putLong("beacon_id", id)
                            }
                        )
                    }

                    host.refreshBeaconList()
                }
            }

        }
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.findNavController().navigateWithAnimation(R.id.beacon_list)
        return true
    }

}
