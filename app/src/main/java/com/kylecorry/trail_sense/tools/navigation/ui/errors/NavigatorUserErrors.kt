package com.kylecorry.trail_sense.tools.navigation.ui.errors

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.ErrorBannerReason
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.requireMainActivity
import com.kylecorry.trail_sense.shared.views.UserError
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.GPSDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.MagnetometerDiagnosticScanner
import java.util.Locale

class NavigatorUserErrors(
    private val host: TrailSenseDestinationHost,
    private val onDisplayAccuracyTips: () -> Unit
) {

    private val banner = host.requireMainActivity().errorBanner
    private val formatter = FormatService.getInstance(host.requireContext())

    private val possibleErrors = listOf(
        ErrorBannerReason.NoGPS,
        ErrorBannerReason.LocationNotSet,
        ErrorBannerReason.GPSTimeout,
        ErrorBannerReason.NoCompass,
        ErrorBannerReason.CompassPoor
    )

    private val errorMap = mapOf(
        GPSDiagnosticScanner.GPS_UNAVAILABLE to UserError(
            ErrorBannerReason.NoGPS,
            host.getString(R.string.location_disabled),
            TrailIcons.Satellite
        ),
        GPSDiagnosticScanner.LOCATION_UNSET to UserError(
            ErrorBannerReason.LocationNotSet,
            host.getString(R.string.location_not_set),
            TrailIcons.Satellite,
            host.getString(R.string.set)
        ) {
            val navController = host.findNavController()
            banner.dismiss(ErrorBannerReason.LocationNotSet)
            navController.navigate(R.id.calibrateGPSFragment)
        },
        GPSDiagnosticScanner.GPS_TIMED_OUT to UserError(
            ErrorBannerReason.GPSTimeout,
            host.getString(R.string.gps_signal_lost),
            TrailIcons.Satellite
        ),
        MagnetometerDiagnosticScanner.MAGNETOMETER_POOR to UserError(
            ErrorBannerReason.CompassPoor,
            host.getString(
                R.string.compass_calibrate_toast,
                formatter.formatQuality(Quality.Poor).lowercase(Locale.getDefault())
            ),
            TrailIcons.CompassIcon,
            host.getString(R.string.how)
        ) {
            onDisplayAccuracyTips()
            banner.dismiss(ErrorBannerReason.CompassPoor)
        },
        MagnetometerDiagnosticScanner.MAGNETOMETER_UNAVAILABLE to UserError(
            ErrorBannerReason.NoCompass,
            host.getString(R.string.no_sensor_message, host.getString(R.string.pref_compass_sensor_title).lowercase()),
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
        if (!isGPSErrorShown && diagnosticResultIds.contains(GPSDiagnosticScanner.LOCATION_UNSET)) {
            show(GPSDiagnosticScanner.LOCATION_UNSET)
            isGPSErrorShown = true
        } else if (!diagnosticResultIds.contains(GPSDiagnosticScanner.LOCATION_UNSET)) {
            banner.dismiss(ErrorBannerReason.LocationNotSet)
            // Do not reset isGPSErrorShown
        }

        // GPS unavailable
        if (!isGPSErrorShown && diagnosticResultIds.contains(GPSDiagnosticScanner.GPS_UNAVAILABLE)) {
            show(GPSDiagnosticScanner.GPS_UNAVAILABLE)
            isGPSErrorShown = true
        } else if (!diagnosticResultIds.contains(GPSDiagnosticScanner.GPS_UNAVAILABLE)) {
            banner.dismiss(ErrorBannerReason.NoGPS)
            // Do not reset isGPSErrorShown
        }

        // GPS timed out
        if (!isTimedOut && diagnosticResultIds.contains(GPSDiagnosticScanner.GPS_TIMED_OUT)) {
            show(GPSDiagnosticScanner.GPS_TIMED_OUT)
            isTimedOut = true
        } else if (!diagnosticResultIds.contains(GPSDiagnosticScanner.GPS_TIMED_OUT)) {
            banner.dismiss(ErrorBannerReason.GPSTimeout)
            isTimedOut = false
        }

        // Compass unavailable
        if (!isCompassErrorShown && diagnosticResultIds.contains(MagnetometerDiagnosticScanner.MAGNETOMETER_UNAVAILABLE)) {
            show(MagnetometerDiagnosticScanner.MAGNETOMETER_UNAVAILABLE)
            isCompassErrorShown = true

        } else if (!diagnosticResultIds.contains(MagnetometerDiagnosticScanner.MAGNETOMETER_UNAVAILABLE)) {
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
