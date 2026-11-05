package com.kylecorry.trail_sense.tools.clinometer.ui

import com.kylecorry.sol.units.Distance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class ClinometerViewModelTest {

    private var time = Instant.ofEpochMilli(0)
    private val viewModel = ClinometerViewModel(
        baselineDistance = { Distance.meters(20f) },
        now = { time }
    )

    @Test
    fun startsFromTheBaselineDistance() {
        viewModel.onResume()
        assertEquals(Distance.meters(20f), viewModel.distanceAway)

        viewModel.measureDistance(Distance.meters(2f))
        viewModel.onResume()
        assertNull(viewModel.distanceAway)
        assertEquals(Distance.meters(2f), viewModel.knownHeight)
    }

    @Test
    fun aTapLocksTheFirstAngle() {
        assertTrue(viewModel.startHold(angle = 10f, incline = 10f))
        assertFalse(viewModel.startHold(angle = 12f, incline = 12f))

        assertEquals(ClinometerHoldResult.SingleAngle, viewModel.endHold(angle = 10.1f, incline = 10.1f))

        assertTrue(viewModel.isLocked)
        assertEquals(10f, viewModel.displayedIncline(30f))
        assertEquals(10f, viewModel.displayedAngle(30f))
    }

    @Test
    fun aTapOnALockClearsIt() {
        viewModel.startHold(angle = 10f, incline = 10f)
        viewModel.endHold(angle = 10f, incline = 10f)

        viewModel.startHold(angle = 20f, incline = 20f)
        assertEquals(ClinometerHoldResult.Cleared, viewModel.endHold(angle = 20f, incline = 20f))

        assertFalse(viewModel.isLocked)
        assertEquals(30f, viewModel.displayedIncline(30f))
    }

    @Test
    fun aSweepLocksBothEnds() {
        viewModel.startHold(angle = -5f, incline = -5f)
        // While held, the live reading shows
        assertEquals(15f, viewModel.displayedIncline(15f))
        time = time.plusMillis(500)

        assertEquals(ClinometerHoldResult.Sweep, viewModel.endHold(angle = 15f, incline = 15f))

        assertEquals(15f, viewModel.displayedIncline(40f))
        assertEquals(-5f, viewModel.lockedIncline1)
        assertEquals(15f, viewModel.lockedIncline2)
    }

    @Test
    fun aQuickSweepIsATap() {
        viewModel.startHold(angle = -5f, incline = -5f)
        time = time.plusMillis(100)

        assertEquals(ClinometerHoldResult.SingleAngle, viewModel.endHold(angle = 15f, incline = 15f))
        assertNull(viewModel.lockedIncline2)
    }

    @Test
    fun endingWithoutAHoldDoesNothing() {
        assertEquals(ClinometerHoldResult.None, viewModel.endHold(angle = 0f, incline = 0f))
    }
}
