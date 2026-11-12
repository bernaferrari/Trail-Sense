package com.kylecorry.trail_sense.tools.tides.domain.commands

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.ITideTableRepo

class LoadAllTideTablesCommand() {

    private val repo = getAppService<ITideTableRepo>()

    suspend fun execute(): List<TideTable> = onIO {
        repo.getTideTables()
    }

}
