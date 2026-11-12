package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.events.EventData

// TODO: Should receivers return a bundle that can be passed to the next receiver?
interface Action {
    suspend fun onReceive(context: Context, data: EventData)
}
