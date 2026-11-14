package com.kylecorry.andromeda.sense.orientation

import com.kylecorry.sol.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GravityAttitudeTest {
    @Test
    fun mapsGravityToWorldUpIncludingUpsideDown() {
        for (gravity in listOf(Vector3(0f, 0f, 9.81f), Vector3(9.81f, 0f, 0f), Vector3(0f, 0f, -9.81f), Vector3(3f, -5f, 8f))) {
            val up = assertNotNull(gravityAttitude(gravity)).rotate(gravity.normalize())
            assertEquals(0f, up.x, 0.001f)
            assertEquals(0f, up.y, 0.001f)
            assertEquals(1f, up.z, 0.001f)
        }
        assertNull(gravityAttitude(Vector3.zero))
    }
}
