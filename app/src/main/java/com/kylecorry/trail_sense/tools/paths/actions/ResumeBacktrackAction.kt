package com.kylecorry.trail_sense.tools.paths.actions

import android.content.Context
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Action
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCatalog

class ResumeBacktrackAction : Action {
    override suspend fun onReceive(context: Context, data: EventData) {
        val service = getAppService<ToolCatalog>().getService(PathsToolRegistration.SERVICE_BACKTRACK)
        service?.restart()
    }
}
