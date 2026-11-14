package com.kylecorry.trail_sense.tools.paths.infrastructure.commands

import android.content.Context
import android.os.SystemClock
import com.kylecorry.andromeda.core.time.SystemTimeProvider
import com.kylecorry.andromeda.sense.readAll
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.CoroutineCommand
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.networkQuality
import com.kylecorry.trail_sense.shared.safeRoundPlaces
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.gps.age
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.BacktrackRecorder
import com.kylecorry.trail_sense.tools.paths.infrastructure.alerts.BacktrackAlerter
import kotlin.time.Clock

class BacktrackCommand(private val context: Context) : CoroutineCommand {

    private val prefs = UserPreferences(context)

    private val sensorService = getAppService<SensorService>()
    private val gps = sensorService.getGPS(SensorService.SINGLE_FIX_GPS_FREQUENCY, tag = TAG)
    private val altimeter = sensorService.getAltimeter(gps = gps)
    private val cellSignalSensor =
        if (prefs.paths.backtrackSaveCellHistory) sensorService.getCellSignal() else null

    private val pathService = getAppService<IPathService>()

    override suspend fun execute() = onDefault {
        val start = SystemClock.elapsedRealtime()
        updateSensors()
        val point = recordWaypoint()
        val outcome = if (point == null) "Skipped" else "Recorded"
        getAppService<Logger>().info(
            TAG,
            "$outcome backtrack point in ${SystemClock.elapsedRealtime() - start}ms " +
                "(GPS: ${gps.hasValidReading}, Altitude: ${altimeter.hasValidReading}, " +
                "Cell Signal: ${cellSignalSensor?.hasValidReading ?: "N/A"})"
        )
        if (point != null) {
            CreateLastSignalBeaconCommand().execute(point)
        }
        showNotification()
    }

    private suspend fun showNotification() {
        val backtrackId = pathService.getBacktrackPathId() ?: return
        val backtrackPath = pathService.getPath(backtrackId) ?: return
        BacktrackAlerter(context).alert(backtrackPath)
    }

    private suspend fun updateSensors() {
        readAll(
            listOfNotNull(gps, altimeter, cellSignalSensor),
            timeout = SensorService.GPS_READ_TIMEOUT,
            forceStopOnCompletion = true
        )

        if (!gps.hasValidReading || (gps as? CustomGPS)?.isTimedOut == true) {
            val reason = if (gps.hasValidReading) "timed out" else "no valid reading"
            val outcome = if (gps.hasValidReading) "recording" else "skipping"
            getAppService<Logger>().warn(
                TAG,
                "GPS did not receive a fix ($reason), $outcome backtrack point from a reading " +
                    "${gps.age(SystemTimeProvider()).inWholeSeconds}s old with ${gps.horizontalAccuracy?.safeRoundPlaces(1)}m accuracy"
            )
        }
    }


    private suspend fun recordWaypoint(): PathPoint? {
        return onIO {
            if (!gps.hasValidReading) {
                return@onIO null
            }

            val waypoint = PathPoint(
                0,
                0,
                gps.location,
                altimeter.altitude,
                Clock.System.now(),
                cellSignalSensor?.networkQuality()
            )

            // Backtrack may have been stopped while waiting for a GPS fix.
            val recorded = BacktrackRecorder(pathService).record(
                waypoint,
                prefs.paths.backtrackEnabled,
                prefs.paths.backtrackMinimumDistance
            )
            if (recorded) waypoint else null
        }
    }

    companion object {
        private const val TAG = "BacktrackCommand"
    }

}
