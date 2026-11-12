package com.kylecorry.trail_sense.tools.pedometer.infrastructure.stride_length

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.sensors.SensorService

class StrideLengthEstimatorFactory() {

    private val sensors = getAppService<SensorService>()

    fun getEstimator(): IStrideLengthEstimator {
        return EndPointStrideLengthEstimator(
            sensors.getGPS(tag = "StepLengthEstimatorFactory"),
            sensors.getPedometer()
        )
    }

}
