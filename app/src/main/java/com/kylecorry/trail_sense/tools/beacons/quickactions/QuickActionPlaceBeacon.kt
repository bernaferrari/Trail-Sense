package com.kylecorry.trail_sense.tools.beacons.quickactions

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.os.Bundle
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.screenControllerAs
import com.google.android.material.snackbar.Snackbar
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.withCancelableLoading
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.ui.list.BeaconListControllerHost
import java.time.ZonedDateTime
import kotlinx.coroutines.launch

class QuickActionPlaceBeacon(btn: QuickActionButtonView, host: TrailSenseDestinationHost) :
    QuickActionButton(btn, host) {

    override fun onCreate() {
        super.onCreate()
        setIcon(TrailIcons.Location)
    }

    override fun onClick() {
        super.onClick()
        host.inBackground {

            var wasSuccessful = false
            var id = 0L
            val job = launch {
                val (location, elevation) = SensorSubsystem.getInstance(host.requireContext())
                    .getLocationAndElevation(
                        SensorSubsystem.SensorRefreshPolicy.Refresh,
                        timeout = SensorService.EXTENDED_GPS_READ_TIMEOUT
                    )

                // Create a beacon
                val formatter = FormatService.getInstance(host.requireContext())
                val time = formatter.formatDateTime(ZonedDateTime.now())

                val prefs = UserPreferences(host.requireContext())
                val beaconService = BeaconService(host.requireContext())
                id = beaconService.add(
                    Beacon(
                        0,
                        time,
                        location,
                        elevation = elevation.meters().value,
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
                        host.findNavigator().navigate(Route.BeaconDetails(beaconId = id))
                    }

                    host.screenControllerAs<BeaconListControllerHost>()?.refreshBeaconList()
                }
            }

        }
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.findNavigator().navigate(Route.BeaconList())
        return true
    }

}
