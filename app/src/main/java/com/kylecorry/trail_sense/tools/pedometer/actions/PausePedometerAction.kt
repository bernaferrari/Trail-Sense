package com.kylecorry.trail_sense.tools.pedometer.actions

import android.content.Context
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Action

class PausePedometerAction : Action {
    override suspend fun onReceive(context: Context, data: EventData) {
        StepCounterService.stop(context)
    }
}
