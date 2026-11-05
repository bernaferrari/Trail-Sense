package com.kylecorry.trail_sense.tools.tides.ui

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideEstimator
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideTableRepo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.time.Duration
import java.time.ZonedDateTime

class CreateTideViewModelTest {

    private fun newViewModel(includeHarmonic: Boolean = false) = CreateTideViewModel(
        editTideId = 0L,
        tideRepo = mock<TideTableRepo>(),
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
        viewModel.updateTideEntry(entry.copy(time = ZonedDateTime.now()))
        val table = viewModel.getTide()
        assertTrue(viewModel.formIsValid())
        assertEquals(TideEstimator.Clock, table?.estimator)
        assertTrue(viewModel.hasChanges)
    }

    @Test
    fun manualLocalIntervalNeedsIntervalAndLocation() {
        val viewModel = newViewModel()
        viewModel.estimateType = EstimateType.LunitidalIntervalManualLocal
        viewModel.lunitidalInterval = Duration.ofHours(2)
        assertNull(viewModel.getTide())
        viewModel.location = Coordinate(1.0, 2.0)
        val table = viewModel.getTide()
        assertEquals(TideEstimator.LunitidalInterval, table?.estimator)
        assertEquals(Duration.ofHours(2), table?.lunitidalInterval)
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
