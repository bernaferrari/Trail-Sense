package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class ChartHeightTest {

    @Test
    fun shortWindowsGetTheCompactChart() {
        assertEquals(150.dp, chartHeightFor(599.dp))
    }

    @Test
    fun windowsAtLeast600dpTallGetTheTallChart() {
        assertEquals(200.dp, chartHeightFor(600.dp))
        assertEquals(200.dp, chartHeightFor(900.dp))
    }
}
