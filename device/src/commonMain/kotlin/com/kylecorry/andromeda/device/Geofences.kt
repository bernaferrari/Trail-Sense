package com.kylecorry.andromeda.device

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Instant

/** A circular region; [expiresAt] is exclusive when set. */
public data class Geofence(
    val id: String,
    val center: Coordinate,
    val radiusMeters: Float,
    val expiresAt: Instant? = null,
) {
    init {
        require(id.isNotBlank()) { "A geofence requires an ID" }
        require(radiusMeters.isFinite() && radiusMeters > 0f) { "The radius must be positive and finite" }
    }
}

public enum class GeofenceTransitionType { Enter, Exit }

public data class GeofenceTransition(
    val fence: Geofence,
    val type: GeofenceTransitionType,
    val location: LocationReading,
    val time: Instant,
)

/**
 * Detects region transitions while collecting location readings. An initial reading inside a
 * region emits Enter; an initial reading outside does not emit Exit. Each collector keeps its
 * own state. Expired regions and out-of-order location readings are ignored.
 */
public fun Flow<Reading<LocationReading>>.geofenceTransitions(fences: List<Geofence>): Flow<GeofenceTransition> {
    val regions = fences.toList()
    require(regions.map { it.id }.toSet().size == regions.size) { "Geofence IDs must be unique" }
    return flow {
        val inside = mutableMapOf<String, Boolean>()
        var lastTime: Instant? = null
        collect { reading ->
            if (lastTime != null && reading.time < lastTime!!) return@collect
            lastTime = reading.time
            for (fence in regions) {
                if (fence.expiresAt != null && reading.time >= fence.expiresAt) {
                    inside.remove(fence.id)
                    continue
                }
                val nowInside = fence.center.distanceTo(reading.value.coordinate) <= fence.radiusMeters
                val before = inside.put(fence.id, nowInside)
                val transition = when {
                    nowInside && before != true -> GeofenceTransitionType.Enter
                    !nowInside && before == true -> GeofenceTransitionType.Exit
                    else -> null
                }
                if (transition != null) emit(GeofenceTransition(fence, transition, reading.value, reading.time))
            }
        }
    }
}
