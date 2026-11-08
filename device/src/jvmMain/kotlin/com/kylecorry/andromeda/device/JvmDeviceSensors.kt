package com.kylecorry.andromeda.device

import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.time.Duration

internal actual fun createDeviceBackend(): DeviceBackend = object : DeviceBackend {
    override fun pressure(interval: Duration): Flow<Reading<Float>> = emptyFlow()
}
