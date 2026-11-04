package com.kylecorry.trail_sense.tools.sensors.quickactions

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.sense.altitude.FusedAltimeter
import com.kylecorry.andromeda.sense.readAll
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Pressure
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.withCancelableLoading
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import kotlinx.coroutines.launch

class QuickActionRecalibrateAltimeter(btn: QuickActionButtonView, host: TrailSenseDestinationHost) :
    QuickActionButton(btn, host) {

    private val prefs by lazy { UserPreferences(host.requireContext()) }

    override fun onCreate() {
        super.onCreate()
        setIcon(TrailIcons.RefreshAltitude)
    }

    override fun onClick() {
        super.onClick()
        val mode = prefs.altimeterMode
        when (mode) {
            UserPreferences.AltimeterMode.Barometer,
            UserPreferences.AltimeterMode.Override -> {
                val distanceUnits = prefs.baseDistanceUnits
                CustomUiUtils.pickElevation(
                    host.requireContext(),
                    Distance.meters(prefs.altitudeOverride).convertTo(distanceUnits),
                    host.getString(R.string.quick_action_recalibrate_altimeter)
                ) { elevation, cancelled ->
                    if (!cancelled && elevation != null) {
                        host.inBackground {
                            val sensorService = SensorService(host.requireContext())
                            if (mode == UserPreferences.AltimeterMode.Barometer) {
                                val barometer = sensorService.getBarometer()
                                barometer.read()
                                val seaLevelPressure = Meteorology.getSeaLevelPressure(
                                    Pressure.hpa(barometer.pressure),
                                    elevation.meters()
                                )
                                prefs.altitudeOverride = elevation.meters().value
                                prefs.seaLevelPressureOverride = seaLevelPressure.value
                            } else {
                                prefs.altitudeOverride = elevation.meters().value
                            }
                            host.toast(host.getString(R.string.elevation_override_updated_toast))
                        }
                    }
                }
            }

            UserPreferences.AltimeterMode.GPSBarometer,
            UserPreferences.AltimeterMode.DigitalElevationModelBarometer -> {
                host.inBackground {
                    val sensorService = SensorService(host.requireContext())
                    val altimeter = sensorService.getAltimeter(tag = "QuickActionRecalibrateAltimeter")

                    val job = launch {
                        FusedAltimeter.clearCachedCalibration(
                            getAppService<PreferencesSubsystem>().preferences
                        )
                        readAll(listOf(altimeter), SensorService.EXTENDED_GPS_READ_TIMEOUT)
                    }

                    Alerts.withCancelableLoading(
                        host.requireContext(),
                        host.getString(R.string.recalibrating),
                        onCancel = { job.cancel() }
                    ) {
                        job.join()
                        host.toast(host.getString(R.string.done))
                    }
                }
            }

            else -> {
                host.findNavController().navigateWithAnimation(R.id.calibrateAltimeterFragment)
            }
        }
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.findNavController().navigateWithAnimation(R.id.calibrateAltimeterFragment)
        return true
    }
}
