package com.kylecorry.andromeda.preferences

import com.kylecorry.sol.units.Coordinate
import com.russhwolf.settings.Settings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * Typed access to platform settings. Supply a Settings instance to choose a named or encrypted store.
 * @param keyChangeSource registers a callback for changes to any key and returns a function that unregisters it.
 * Platform factories supply this when the backing store can report every change; without it [observeChanges] never emits.
 */
class Preferences(
    val settings: Settings = Settings(),
    private val keyChangeSource: ((onChange: (key: String) -> Unit) -> (() -> Unit))? = null
) {
    /** Emits the key of every preference that changes while collected. */
    fun observeChanges(): Flow<String> {
        val source = keyChangeSource ?: return emptyFlow()
        return callbackFlow {
            val unregister = source { trySend(it) }
            awaitClose { unregister() }
        }
    }

    fun contains(key: String): Boolean = settings.hasKey(key)
    fun remove(key: String) = settings.remove(key)
    fun clear() = settings.clear()
    val keys: Set<String> get() = settings.keys

    fun putInt(key: String, value: Int) = settings.putInt(key, value)
    fun getInt(key: String): Int? = settings.getIntOrNull(key)
    fun putLong(key: String, value: Long) = settings.putLong(key, value)
    fun getLong(key: String): Long? = settings.getLongOrNull(key)
    fun putFloat(key: String, value: Float) = settings.putFloat(key, value)
    fun getFloat(key: String): Float? = settings.getFloatOrNull(key)
    fun putDouble(key: String, value: Double) = settings.putString(key, value.toString())
    fun getDouble(key: String): Double? = settings.getStringOrNull(key)?.replace(',', '.')?.toDoubleOrNull()
    fun putBoolean(key: String, value: Boolean) = settings.putBoolean(key, value)
    fun getBoolean(key: String): Boolean? = settings.getBooleanOrNull(key)
    fun putString(key: String, value: String) = settings.putString(key, value)
    fun getString(key: String): String? = settings.getStringOrNull(key)

    fun putCoordinate(key: String, value: Coordinate) = putString(key, "${value.latitude},${value.longitude}")
    fun getCoordinate(key: String): Coordinate? {
        val parts = getString(key)?.split(',') ?: return null
        if (parts.size != 2) return null
        val latitude = parts[0].toDoubleOrNull() ?: return null
        val longitude = parts[1].toDoubleOrNull() ?: return null
        return Coordinate(latitude, longitude)
    }
    fun putLocalDate(key: String, value: LocalDate) = putString(key, value.toString())
    fun getLocalDate(key: String): LocalDate? = getString(key)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    fun putInstant(key: String, value: Instant) = putLong(key, value.toEpochMilliseconds())
    fun getInstant(key: String): Instant? = getLong(key)?.let(Instant::fromEpochMilliseconds)
    fun putDuration(key: String, value: Duration) = putLong(key, value.inWholeMilliseconds)
    fun getDuration(key: String): Duration? = getLong(key)?.milliseconds
}

fun Preferences.putIntArray(key: String, value: List<Int>) = putString(key, value.joinToString(","))
fun Preferences.getIntArray(key: String): List<Int>? = getString(key)?.split(",")?.mapNotNull(String::toIntOrNull)
fun Preferences.putLongArray(key: String, value: List<Long>) = putString(key, value.joinToString(","))
fun Preferences.getLongArray(key: String): List<Long>? = getString(key)?.split(",")?.mapNotNull(String::toLongOrNull)

fun Preferences.putOrRemoveFloat(key: String, value: Float?) {
    if (value == null) remove(key) else putFloat(key, value)
}
fun Preferences.putOrRemoveCoordinate(key: String, value: Coordinate?) {
    if (value == null) remove(key) else putCoordinate(key, value)
}
