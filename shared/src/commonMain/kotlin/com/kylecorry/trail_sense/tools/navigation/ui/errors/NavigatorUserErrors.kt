package com.kylecorry.trail_sense.tools.navigation.ui.errors

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.trail_sense.shared.ErrorBannerReason
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.views.UserError
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.views.ErrorBannerState
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.SensorDiagnosticIds

/** The compass's sensor problems, shown on the activity's error [banner]. */
class NavigatorUserErrors(
    private val banner: ErrorBannerState,
    private val navigator: TrailSenseNavigator,
    private val onDisplayAccuracyTips: () -> Unit
) {

    private val formatter = FormatService.getInstance()

    private val possibleErrors = listOf(
        ErrorBannerReason.NoGPS,
        ErrorBannerReason.LocationNotSet,
        ErrorBannerReason.GPSTimeout,
        ErrorBannerReason.NoCompass,
        ErrorBannerReason.CompassPoor
    )

    private val errorMap = mapOf(
        SensorDiagnosticIds.GPS_UNAVAILABLE to UserError(
            ErrorBannerReason.NoGPS,
            getAppService<AppStrings>().get(Res.string.location_disabled),
            TrailIcons.Satellite
        ),
        SensorDiagnosticIds.LOCATION_UNSET to UserError(
            ErrorBannerReason.LocationNotSet,
            getAppService<AppStrings>().get(Res.string.location_not_set),
            TrailIcons.Satellite,
            getAppService<AppStrings>().get(Res.string.set)
        ) {
            banner.dismiss(ErrorBannerReason.LocationNotSet)
            navigator.navigate(Route.CalibrateGps)
        },
        SensorDiagnosticIds.GPS_TIMED_OUT to UserError(
            ErrorBannerReason.GPSTimeout,
            getAppService<AppStrings>().get(Res.string.gps_signal_lost),
            TrailIcons.Satellite
        ),
        SensorDiagnosticIds.MAGNETOMETER_POOR to UserError(
            ErrorBannerReason.CompassPoor,
            getAppService<AppStrings>().get(Res.string.compass_calibrate_toast,
                formatter.formatQuality(Quality.Poor).lowercase()
            ),
            TrailIcons.CompassIcon,
            getAppService<AppStrings>().get(Res.string.how)
        ) {
            onDisplayAccuracyTips()
            banner.dismiss(ErrorBannerReason.CompassPoor)
        },
        SensorDiagnosticIds.MAGNETOMETER_UNAVAILABLE to UserError(
            ErrorBannerReason.NoCompass,
            getAppService<AppStrings>().get(Res.string.no_sensor_message, getAppService<AppStrings>().get(Res.string.pref_compass_sensor_title).lowercase()),
            TrailIcons.CompassIcon
        )
    )

    private var isTimedOut = false
    private var isGPSErrorShown = false
    private var isCompassErrorShown = false
    private var isPoorCompassShown = false

    fun reset() {
        isTimedOut = false
        isGPSErrorShown = false
        isCompassErrorShown = false
        isPoorCompassShown = false
        possibleErrors.forEach {
            banner.dismiss(it)
        }
    }

    // TODO: Improve architecture using the strategy pattern and remove this class all together
    // TODO: Each error condition should be responsible for its own display and dismissal
    fun update(diagnosticResultIds: List<String>) {

        // Location unset
        if (!isGPSErrorShown && diagnosticResultIds.contains(SensorDiagnosticIds.LOCATION_UNSET)) {
            show(SensorDiagnosticIds.LOCATION_UNSET)
            isGPSErrorShown = true
        } else if (!diagnosticResultIds.contains(SensorDiagnosticIds.LOCATION_UNSET)) {
            banner.dismiss(ErrorBannerReason.LocationNotSet)
            // Do not reset isGPSErrorShown
        }

        // GPS unavailable
        if (!isGPSErrorShown && diagnosticResultIds.contains(SensorDiagnosticIds.GPS_UNAVAILABLE)) {
            show(SensorDiagnosticIds.GPS_UNAVAILABLE)
            isGPSErrorShown = true
        } else if (!diagnosticResultIds.contains(SensorDiagnosticIds.GPS_UNAVAILABLE)) {
            banner.dismiss(ErrorBannerReason.NoGPS)
            // Do not reset isGPSErrorShown
        }

        // GPS timed out
        if (!isTimedOut && diagnosticResultIds.contains(SensorDiagnosticIds.GPS_TIMED_OUT)) {
            show(SensorDiagnosticIds.GPS_TIMED_OUT)
            isTimedOut = true
        } else if (!diagnosticResultIds.contains(SensorDiagnosticIds.GPS_TIMED_OUT)) {
            banner.dismiss(ErrorBannerReason.GPSTimeout)
            isTimedOut = false
        }

        // Compass unavailable
        if (!isCompassErrorShown && diagnosticResultIds.contains(SensorDiagnosticIds.MAGNETOMETER_UNAVAILABLE)) {
            show(SensorDiagnosticIds.MAGNETOMETER_UNAVAILABLE)
            isCompassErrorShown = true

        } else if (!diagnosticResultIds.contains(SensorDiagnosticIds.MAGNETOMETER_UNAVAILABLE)) {
            banner.dismiss(ErrorBannerReason.NoCompass)
            // Do not reset isCompassErrorShown
        }

        // Compass poor: no banner. The status icon in the top bar already shows it (with a dot and a popover that
        // leads to the tips), and a red banner over the compass for a routine calibration nag is too loud.
        banner.dismiss(ErrorBannerReason.CompassPoor)

    }

    private fun show(diagnosticResultId: String) {
        val error = errorMap[diagnosticResultId]
        if (error != null) {
            banner.report(error)
        }
    }
}
