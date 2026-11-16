package com.kylecorry.trail_sense.tools.tides.domain.loading

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.selection.ITideSelectionStrategy
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.ITideTableRepo

class TideLoaderImpl(
    private val tideRepo: ITideTableRepo,
    private val strategy: ITideSelectionStrategy
) : ITideLoader {
    override suspend fun getTideTable(): TideTable? = onIO {
        val tides = tideRepo.getTideTables()
        strategy.getTide(tides)
    }
}
