package com.kylecorry.trail_sense.shared

import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.signal.CellNetworkQuality
import com.kylecorry.andromeda.signal.CellSignalSensor
import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.data.Identifiable
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import kotlin.math.roundToInt

fun <T : Identifiable> Array<T>.withId(id: Long): T? {
    return firstOrNull { it.id == id }
}


fun <T : Identifiable> Collection<T>.withId(id: Long): T? {
    return firstOrNull { it.id == id }
}


fun GeoUri.Companion.from(beacon: Beacon): GeoUri {
    val params = mutableMapOf(
        "label" to beacon.name
    )
    beacon.elevation?.let { params["ele"] = it.roundPlaces(2).toString() }

    return GeoUri(beacon.coordinate, null, params)
}


fun GeoUri.toBeacon(): Beacon = Beacon(
    id = 0,
    name = queryParameters["label"].orEmpty(),
    coordinate = coordinate,
    elevation = altitude ?: queryParameters["ele"]?.toFloatOrNull(),
    color = AppColor.Orange.color
)


val ZERO_SPEED = Speed.from(0f, DistanceUnits.Meters, TimeUnits.Seconds)


fun CellSignalSensor.networkQuality(): CellNetworkQuality? {
    val signal = signals.maxByOrNull { it.strength }
    return signal?.let { CellNetworkQuality(it.network, it.quality) }
}


fun PixelCoordinate.toVector2(top: Float): Vector2 {
    return Vector2(x, -(y - top))
}


fun Vector2.toPixelCoordinate(top: Float): PixelCoordinate {
    return PixelCoordinate(x, -(y - top))
}


fun Vector2.rotateInRect(angle: Float, currentSize: Size, newSizeOverride: Size? = null): Vector2 {
    val newSize = newSizeOverride ?: currentSize.rotate(angle)
    return minus(Vector2(currentSize.width / 2f, currentSize.height / 2f))
        .rotate(angle)
        .plus(Vector2(newSize.width / 2f, newSize.height / 2f))
}


fun Enum<*>.readableName(): String {
    return name.map { if (it.isUpperCase()) " $it" else it }
        .joinToString("").trim()
}


/** Opens a tool's main screen, as registered in [Tools]. */

inline fun List<Float>.forEachLine(action: (x1: Float, y1: Float, x2: Float, y2: Float) -> Unit) {
    for (i in indices step 4) {
        action(this[i], this[i + 1], this[i + 2], this[i + 3])
    }
}


fun Float.safeRoundToInt(default: Int = 0): Int {
    return tryOrDefault(default) {
        if (isNaN() || isInfinite()) {
            default
        } else {
            roundToInt()
        }
    }
}


fun Float.safeRoundPlaces(places: Int, default: Float = 0f): Float {
    return tryOrDefault(default) {
        if (isNaN() || isInfinite()) {
            default
        } else {
            roundPlaces(places)
        }
    }
}


fun Double.safeRoundToInt(default: Int = 0): Int {
    return tryOrDefault(default) {
        if (isNaN() || isInfinite()) {
            default
        } else {
            roundToInt()
        }
    }
}


fun <T> List<T>.padRight(minLength: Int, value: T): List<T> {
    return if (size >= minLength) {
        this
    } else {
        this + List(minLength - size) { value }
    }
}


fun Long.getUpsertedId(originalId: Long): Long {
    return if (this == -1L) {
        originalId
    } else {
        this
    }
}
