package com.kylecorry.andromeda.sense.location

import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

/** GNSS NMEA messages. Android requires KSensor initialization and fine-location permission;
 * unsupported platforms fail on collection.
 */
expect fun nmeaMessages(): Flow<Reading<Nmea>>

/** Mean-sea-level altitude from NMEA messages that contain it. */
fun nmeaMslAltitudes(): Flow<Reading<Float>> = nmeaMessages().mapNotNull { reading ->
    reading.value.mslAltitude?.let { Reading(it, reading.time) }
}
