package com.kylecorry.trail_sense.tools.tides.ui

import kotlin.time.Duration.Companion.hours
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideEstimator
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.ITideTableRepo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

class CreateTideViewModelTest {

    private fun newViewModel(includeHarmonic: Boolean = false) = CreateTideViewModel(
        editTideId = 0L,
        tideRepo = EmptyTideTableRepo,
        units = DistanceUnits.Meters,
        includeHarmonic = includeHarmonic
    )

    @Test
    fun newFormStartsWithOneEmptyTideAndNothingToSave() {
        val viewModel = newViewModel()
        assertEquals(1, viewModel.tideEntries.size)
        assertFalse(viewModel.formIsValid())
        assertFalse(viewModel.hasChanges)
    }

    @Test
    fun clockTableNeedsATimedTide() {
        val viewModel = newViewModel()
        val entry = viewModel.tideEntries.first()
        viewModel.updateTideEntry(entry.copy(time = Clock.System.now()))
        val table = viewModel.getTide()
        assertTrue(viewModel.formIsValid())
        assertEquals(TideEstimator.Clock, table?.estimator)
        assertTrue(viewModel.hasChanges)
    }

    @Test
    fun manualLocalIntervalNeedsIntervalAndLocation() {
        val viewModel = newViewModel()
        viewModel.estimateType = EstimateType.LunitidalIntervalManualLocal
        viewModel.lunitidalInterval = 2.hours
        assertNull(viewModel.getTide())
        viewModel.location = Coordinate(1.0, 2.0)
        val table = viewModel.getTide()
        assertEquals(TideEstimator.LunitidalInterval, table?.estimator)
        assertEquals(2.hours, table?.lunitidalInterval)
        assertFalse(table!!.lunitidalIntervalIsUtc)
    }

    @Test
    fun deletedTideCanBeRestored() {
        val viewModel = newViewModel()
        val entry = viewModel.tideEntries.first()
        viewModel.deleteTideEntry(entry)
        assertTrue(viewModel.tideEntries.isEmpty())
        viewModel.restoreTideEntry(entry)
        assertEquals(listOf(entry), viewModel.tideEntries)
    }

    @Test
    fun harmonicOnlyOfferedWhenIncluded() {
        assertFalse(EstimateType.Harmonic in newViewModel().estimateTypes)
        assertTrue(EstimateType.Harmonic in newViewModel(includeHarmonic = true).estimateTypes)
    }
}

private object EmptyTideTableRepo : ITideTableRepo {
    override suspend fun getTideTables(): List<TideTable> = emptyList()
    override suspend fun getTideTable(id: Long): TideTable? = null
    override suspend fun addTideTable(table: TideTable): Long = 0
    override suspend fun deleteTideTable(table: TideTable) {}
    override suspend fun addTides(tableId: Long, tides: List<Tide>) {}
    override suspend fun deleteTides(tableId: Long) {}
}
