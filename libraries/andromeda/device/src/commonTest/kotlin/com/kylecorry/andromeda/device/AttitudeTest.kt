package com.kylecorry.andromeda.device

import com.kylecorry.sol.math.Quaternion
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals

class AttitudeTest {
    @Test
    fun rotatesNorthWestReferenceIntoEastNorthReference() {
        val half = sqrt(0.5f)
        val eastNorthAligned = northWestUpToEastNorthUp(Quaternion(0f, 0f, -half, half))
        assertEquals(0f, eastNorthAligned.x, 0.00001f)
        assertEquals(0f, eastNorthAligned.y, 0.00001f)
        assertEquals(0f, eastNorthAligned.z, 0.00001f)
        assertEquals(1f, eastNorthAligned.w, 0.00001f)
    }
}
