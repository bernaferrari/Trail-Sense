package com.kylecorry.sol.math.filters

public class KalmanFilter(
    initialEstimate: Double,
    private val initialError: Double,
    private val processError: Double,
    initialTime: Double? = null
) {

    public var estimate: Double = initialEstimate
        private set
    public var estimateError: Double = initialError
        private set
    private var lastTime = initialTime

    public fun filter(measurement: Double, error: Double = initialError, time: Double? = null): Double {

        val timeScale = if (lastTime != null && time != null) {
            time - (lastTime ?: 0.0)
        } else {
            1.0
        }

        lastTime = time

        var divisor = (estimateError + error)
        if (divisor == 0.0) {
            divisor = 0.0001
        }

        val kg = estimateError / divisor
        estimate += kg * (measurement - estimate)
        estimateError = (1 - kg) * (estimateError + processError * timeScale)
        return estimate
    }

    public companion object {

        /**
         * Filter a list of measurements, including the error estimate with the estimate
         * @return a list of pairs of estimate to error
         */
        public fun filterWithError(
            measurements: List<Double>,
            errors: List<Double>,
            processError: Double,
            times: List<Double>? = null
        ): List<Pair<Double, Double>> {
            if (measurements.isEmpty()) {
                return emptyList()
            } else if (measurements.size < 2) {
                return listOf(measurements[0] to errors[0])
            }

            val kalman = KalmanFilter(measurements[0], errors[0], processError, times?.get(0))
            val values = mutableListOf(measurements[0] to errors[0])

            for (i in 1..measurements.lastIndex) {
                values.add(
                    kalman.filter(
                        measurements[i],
                        errors[i],
                        times?.get(i)
                    ) to kalman.estimateError
                )
            }

            return values
        }

        public fun filter(
            measurements: List<Double>,
            errors: List<Double>,
            processError: Double,
            times: List<Double>? = null
        ): List<Double> {
            if (measurements.isEmpty()) {
                return emptyList()
            } else if (measurements.size < 2) {
                return listOf(measurements[0])
            }

            val kalman = KalmanFilter(measurements[0], errors[0], processError, times?.get(0))
            val values = mutableListOf(measurements[0])

            for (i in 1..measurements.lastIndex) {
                values.add(kalman.filter(measurements[i], errors[i], times?.get(i)))
            }

            return values
        }

        public fun filter(
            measurements: List<Double>,
            error: Double,
            processError: Double,
            times: List<Double>? = null
        ): List<Double> {
            return filter(measurements, List(measurements.size) { error }, processError, times)
        }

    }

}
