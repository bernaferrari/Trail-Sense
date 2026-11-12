package com.kylecorry.trail_sense.tools.weather.actions

import android.content.Context
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.tools.tools.infrastructure.Action
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCatalog
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration

class PauseWeatherMonitorAction : Action {
    override suspend fun onReceive(context: Context, data: EventData) {
        val service = getAppService<ToolCatalog>().getService(WeatherToolRegistration.SERVICE_WEATHER_MONITOR)
        service?.stop()
    }
}
