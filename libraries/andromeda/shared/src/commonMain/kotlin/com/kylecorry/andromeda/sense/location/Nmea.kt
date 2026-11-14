package com.kylecorry.andromeda.sense.location


public data class Nmea(val message: String) {
    val mslAltitude: Float?
        get() {
            val validProtocols = listOf("\$GPGGA", "\$GNGNS", "\$GNGGA")
            val containsMslAltitude = validProtocols.any { message.startsWith(it) }

            if (!containsMslAltitude) {
                return null
            }

            return message.split(',').getOrNull(9)?.toFloatOrNull()
        }
}