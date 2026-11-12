package com.kylecorry.trail_sense.tools.tides.domain.commands

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.loading.TideLoaderFactory

class LoadTideTableCommand {

    suspend fun execute(): TideTable? = onIO {
        val loader = TideLoaderFactory().getTideLoader()
        loader.getTideTable()
    }

}
