package com.kylecorry.andromeda.sense.location

data class Satellite(
    val id: Int,
    val constellation: SatelliteConstellation?,
    val elevation: Float,
    val azimuth: Float,
    val usedInFix: Boolean
) {
    companion object
}

enum class SatelliteConstellation { GPS, SBAS, GLONASS, QZSS, BEIDOU, GALILEO, IRNSS }
