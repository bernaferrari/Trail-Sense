package com.kylecorry.trail_sense.tools.tides.domain.commands

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.ITideTableRepo

class ToggleTideTableVisibilityCommand() {

    private val repo = getAppService<ITideTableRepo>()

    suspend fun execute(table: TideTable): TideTable {
        val newTable = table.copy(isVisible = !table.isVisible)
        onIO {
            repo.addTideTable(newTable)
        }
        return newTable
    }

}
