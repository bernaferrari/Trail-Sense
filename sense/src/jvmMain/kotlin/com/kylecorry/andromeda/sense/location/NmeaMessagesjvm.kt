package com.kylecorry.andromeda.sense.location

import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

public actual fun nmeaMessages(): Flow<Reading<Nmea>> = flow {
    throw UnsupportedOperationException("NMEA messages are unavailable on this platform")
}
