package com.kylecorry.andromeda.device

import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Cross-platform device observations. */
class DeviceSensors {
    internal val backend = createDeviceBackend()

    fun barometer(scope: CoroutineScope, interval: Duration = 1.seconds) =
        FlowBarometer(pressure(interval), scope)

    /** Atmospheric pressure in hectopascals. */
    fun pressure(interval: Duration = 1.seconds): Flow<Reading<Float>> = backend.pressure(interval)
}

internal interface DeviceBackend {
    fun pressure(interval: Duration): Flow<Reading<Float>>
}

internal expect fun createDeviceBackend(): DeviceBackend
