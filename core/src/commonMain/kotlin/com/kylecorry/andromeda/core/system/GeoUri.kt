package com.kylecorry.andromeda.core.system

import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.units.Coordinate
import io.ktor.http.decodeURLPart
import io.ktor.http.encodeURLParameter

data class GeoUri(
    val coordinate: Coordinate,
    val altitude: Float? = null,
    val queryParameters: Map<String, String> = emptyMap(),
) {
    override fun toString(): String {
        val base = "geo:${coordinate.latitude.roundPlaces(6)},${coordinate.longitude.roundPlaces(6)}"
        val elevation = altitude?.let { ",${it.roundPlaces(2)}" }.orEmpty()
        val query = if (queryParameters.isEmpty()) "" else queryParameters.entries.joinToString("&", prefix = "?") {
            "${it.key.encodeURLParameter()}=${it.value.encodeURLParameter()}"
        }
        return base + elevation + query
    }

    companion object {
        fun parse(uri: String): GeoUri? {
            val pattern = Regex("geo:(-?[0-9]*\\.?[0-9]+),(-?[0-9]*\\.?[0-9]+)(?:,(-?[0-9]*\\.?[0-9]+))?(?:\\?(.*))?")
            val match = pattern.find(uri) ?: return null
            val latitude = match.groupValues[1].toDoubleOrNull() ?: return null
            val longitude = match.groupValues[2].toDoubleOrNull() ?: return null
            val altitude = match.groupValues[3].toFloatOrNull()
            val query = match.groupValues[4].split('&').mapNotNull { parameter ->
                val parts = parameter.split('=', limit = 2)
                if (parts.size == 2) parts[0].decodeURLPart() to parts[1].decodeURLPart() else null
            }.toMap()
            return GeoUri(Coordinate(latitude, longitude), altitude, query)
        }
    }
}
