package com.kylecorry.andromeda.device

import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal actual fun platformSatelliteStatus(): Flow<Reading<List<Satellite>>> = emptyFlow()
