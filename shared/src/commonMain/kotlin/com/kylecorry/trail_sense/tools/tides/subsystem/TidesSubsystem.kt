package com.kylecorry.trail_sense.tools.tides.subsystem

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.sensors.ILocationSubsystem
import com.kylecorry.trail_sense.shared.time.today
import com.kylecorry.trail_sense.tools.tides.domain.TideDetails
import com.kylecorry.trail_sense.tools.tides.domain.TideLocationKey
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.commands.CurrentTideCommand
import com.kylecorry.trail_sense.tools.tides.domain.commands.DailyTideCommand
import com.kylecorry.trail_sense.tools.tides.domain.loading.TideLoaderFactory
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideEstimator
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideHarmonicsSource
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import com.kylecorry.trail_sense.tools.tides.ui.DailyTideData
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate

class TidesSubsystem private constructor(harmonicsSource: TideHarmonicsSource) {

    private val tideService = TideService(harmonicsSource)
    private val tideLoaderFactory = TideLoaderFactory()

    private var lastTable: TideTable? = null
    private var lastLocation: Coordinate? = null
    private var lastDate: LocalDate? = null
    private var lastDailyTide: DailyTideData? = null
    private val mutex = Mutex()

    suspend fun getNearestTide(location: Coordinate? = null): TideDetails? {
        val table = getTideTable(location) ?: return null
        // Automatic nearby tides require on the fly locations rather than the location being part of the model
        val resolvedLocation = if (table.isAutomaticNearbyTide) {
            TideLocationKey.of(location ?: getAppService<ILocationSubsystem>().location)
        } else {
            null
        }
        val tide = CurrentTideCommand(tideService).execute(table)
        val times = mutex.withLock {
            if (isDailyStillValid(table, resolvedLocation)) {
                lastDailyTide!!
            } else {
                val newDaily = DailyTideCommand(tideService).execute(table, today())
                lastDailyTide = newDaily
                lastTable = table
                lastLocation = resolvedLocation
                lastDate = today()
                newDaily
            }
        }

        if (times.waterLevels.all { it.value == 0f }) {
            return null
        }

        return TideDetails(table, tide, times)
    }

    private suspend fun getTideTable(location: Coordinate?): TideTable? {
        val loader = tideLoaderFactory.getTideLoader(false, location)
        val table = loader.getTideTable() ?: return null

        if (table.isEditable || location == null) {
            return table
        }

        return TideTable(
            -1,
            emptyList(),
            null,
            estimator = TideEstimator.TideModel,
            isEditable = false,
            location = location
        )
    }

    private fun isDailyStillValid(table: TideTable, location: Coordinate?): Boolean {
        if (lastDate == null || lastDailyTide == null) {
            return false
        }

        if (table != lastTable || location != lastLocation) {
            return false
        }

        return lastDate == today()
    }

    companion object {
        private var instance: TidesSubsystem? = null
        private val lock = SynchronizedObject()

        fun getInstance(): TidesSubsystem = synchronized(lock) {
            instance ?: TidesSubsystem(getAppService<TideHarmonicsSource>()).also { instance = it }
        }
    }

}
