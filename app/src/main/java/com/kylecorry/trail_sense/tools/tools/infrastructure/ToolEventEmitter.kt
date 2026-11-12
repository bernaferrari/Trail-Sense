package com.kylecorry.trail_sense.tools.tools.infrastructure

import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.IEventEmitter
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts

object ToolEventEmitter : IEventEmitter {

    override fun broadcast(eventId: String, data: EventData?) {
        ToolBroadcasts.broadcast(eventId, data)
    }
}
