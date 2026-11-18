package com.kylecorry.trail_sense.tools.paths.infrastructure.commands

import com.kylecorry.andromeda.sense.readAll
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import kotlin.time.Clock

class AddPathPointCommand(
    private val pathId: Long
) {

    private val sensorService = getAppService<SensorService>()
    private val gps = sensorService.getGPS(SensorService.SINGLE_FIX_GPS_FREQUENCY, tag = TAG)
    private val altimeter = sensorService.getAltimeter(gps = gps)

    private val pathService = getAppService<IPathService>()

    suspend fun execute(): Boolean = onDefault {
        readAll(
            listOf(gps, altimeter),
            timeout = SensorService.GPS_READ_TIMEOUT,
            forceStopOnCompletion = true
        )

        if (!gps.hasValidReading) return@onDefault false
        val point = PathPoint(0, pathId, gps.location, altimeter.altitude, Clock.System.now())
        onIO { pathService.addWaypoint(point) }
        true
    }

    companion object {
        private const val TAG = "AddPathPointCommand"
    }

}
