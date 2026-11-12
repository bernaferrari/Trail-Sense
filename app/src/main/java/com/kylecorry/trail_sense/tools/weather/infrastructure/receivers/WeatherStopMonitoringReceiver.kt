package com.kylecorry.trail_sense.tools.weather.infrastructure.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCatalog
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WeatherStopMonitoringReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                getAppService<ToolCatalog>().getService(WeatherToolRegistration.SERVICE_WEATHER_MONITOR)
                    ?.disable()
            } finally {
                pendingResult.finish()
            }
        }
    }

}
