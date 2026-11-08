package com.kylecorry.andromeda.device

import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class FlowGyroscopeTest {
    @Test
    fun integratesAngularVelocityAndReportsActualRate() = runBlocking {
        val first = Instant.fromEpochSeconds(10)
        val readings = flowOf(
            Reading(Vector3(0f, 0f, 90f), first),
            Reading(Vector3(0f, 0f, 90f), Instant.fromEpochSeconds(11)),
        )
        val gyro = FlowGyroscope(readings, this)
        val finished = CompletableDeferred<Unit>()
        var count = 0
        val listener = { if (++count == 2) finished.complete(Unit); true }
        gyro.start(listener)
        withTimeout(1000) { finished.await() }
        val half = sqrt(0.5f)
        assertEquals(half, gyro.orientation.z, 0.0001f)
        assertEquals(half, gyro.orientation.w, 0.0001f)
        assertEquals(90f, gyro.angularRate.yaw, 0.0001f)
        gyro.stop(listener)
    }
}
