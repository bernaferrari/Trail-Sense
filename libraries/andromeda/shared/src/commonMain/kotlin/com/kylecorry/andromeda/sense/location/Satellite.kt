package com.kylecorry.andromeda.sense.location

public data class Satellite(
    val id: Int,
    val constellation: SatelliteConstellation?,
    val elevation: Float,
    val azimuth: Float,
    val usedInFix: Boolean
) {
    public companion object
}

public enum class SatelliteConstellation { GPS, SBAS, GLONASS, QZSS, BEIDOU, GALILEO, IRNSS }
