package com.kylecorry.andromeda.sense.orientation

import com.kylecorry.sol.math.Vector3
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GeomagneticAttitudeTest {
    @Test
    fun producesValidAttitudeAtHalfTurnAndRejectsDegenerateInputs() {
        val gravity = Vector3(0f, 0f, 9.81f)
        val north = assertNotNull(geomagneticAttitude(gravity, Vector3(0f, 50f, 0f)))
        assertEquals(1f, abs(north.w), 0.0001f)
        val south = assertNotNull(geomagneticAttitude(gravity, Vector3(0f, -50f, 0f)))
        assertEquals(1f, abs(south.z), 0.0001f)
        assertEquals(0f, south.w, 0.0001f)
        assertNull(geomagneticAttitude(Vector3.zero, Vector3(0f, 50f, 0f)))
        assertNull(geomagneticAttitude(gravity, Vector3(0f, 0f, 50f)))
    }
}
