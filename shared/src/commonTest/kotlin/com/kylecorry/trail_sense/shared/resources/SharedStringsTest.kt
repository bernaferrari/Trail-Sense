package com.kylecorry.trail_sense.shared.resources

import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.back
import com.kylecorry.trail_sense.shared.generated.resources.convert_swap_units
import com.kylecorry.trail_sense.shared.generated.resources.horizon
import com.kylecorry.trail_sense.shared.generated.resources.no_data
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals

/** The strings synced from the app's res folder resolve in common code (the test environment is English). */
@OptIn(ExperimentalResourceApi::class)
class SharedStringsTest {
    @Test
    fun syncedAppStringsResolve() = runTest {
        assertEquals("Back", getString(Res.string.back))
        assertEquals("No data", getString(Res.string.no_data))
        assertEquals("Swap units", getString(Res.string.convert_swap_units))
        assertEquals("Horizon", getString(Res.string.horizon))
    }
}
