package com.kylecorry.trail_sense.tools.weather.infrastructure

import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.alerts.ILoadingIndicator
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.IWeatherSubsystem
import kotlin.time.Duration

class WeatherLogger(
    private val interval: Duration,
    private val initialDelay: Duration = Duration.ZERO,
    private val loadingIndicator: ILoadingIndicator? = null
) {
    private val weather = getAppService<IWeatherSubsystem>()
    private val runner = CoroutineQueueRunner()
    private val timer = CoroutineTimer {
        onIO {
            runner.skipIfRunning {
                onMain { loadingIndicator?.show() }
                weather.updateWeather()
                onMain { loadingIndicator?.hide() }
            }
        }
    }

    fun start() {
        timer.interval(interval, initialDelay)
    }

    fun stop() {
        timer.stop()
        runner.cancel()
    }

}
