package com.kylecorry.andromeda.sense.location

import androidx.core.location.GnssStatusCompat

fun Satellite.Companion.fromStatus(status: GnssStatusCompat): List<Satellite> =
    (0 until status.satelliteCount).map { fromStatus(status, it) }

fun Satellite.Companion.fromStatus(status: GnssStatusCompat, index: Int): Satellite = Satellite(
    status.getSvid(index),
    when (status.getConstellationType(index)) {
        GnssStatusCompat.CONSTELLATION_GPS -> SatelliteConstellation.GPS
        GnssStatusCompat.CONSTELLATION_SBAS -> SatelliteConstellation.SBAS
        GnssStatusCompat.CONSTELLATION_GLONASS -> SatelliteConstellation.GLONASS
        GnssStatusCompat.CONSTELLATION_QZSS -> SatelliteConstellation.QZSS
        GnssStatusCompat.CONSTELLATION_BEIDOU -> SatelliteConstellation.BEIDOU
        GnssStatusCompat.CONSTELLATION_GALILEO -> SatelliteConstellation.GALILEO
        GnssStatusCompat.CONSTELLATION_IRNSS -> SatelliteConstellation.IRNSS
        else -> null
    },
    status.getElevationDegrees(index),
    status.getAzimuthDegrees(index),
    status.usedInFix(index)
)
