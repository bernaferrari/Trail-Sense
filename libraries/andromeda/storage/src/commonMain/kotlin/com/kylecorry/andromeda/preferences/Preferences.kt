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
public class Preferences(
    public val settings: Settings = Settings(),
    private val keyChangeSource: ((onChange: (key: String) -> Unit) -> (() -> Unit))? = null
) {
    /** Emits the key of every preference that changes while collected. */
    public fun observeChanges(): Flow<String> {
        val source = keyChangeSource ?: return emptyFlow()
        return callbackFlow {
            val unregister = source { trySend(it) }
            awaitClose { unregister() }
        }
    }

    public fun contains(key: String): Boolean = settings.hasKey(key)
    public fun remove(key: String): Unit = settings.remove(key)
    public fun clear(): Unit = settings.clear()
    public val keys: Set<String> get() = settings.keys

    public fun putInt(key: String, value: Int): Unit = settings.putInt(key, value)
    public fun getInt(key: String): Int? = settings.getIntOrNull(key)
    public fun putLong(key: String, value: Long): Unit = settings.putLong(key, value)
    public fun getLong(key: String): Long? = settings.getLongOrNull(key)
    public fun putFloat(key: String, value: Float): Unit = settings.putFloat(key, value)
    public fun getFloat(key: String): Float? = settings.getFloatOrNull(key)
    public fun putDouble(key: String, value: Double): Unit = settings.putString(key, value.toString())
    public fun getDouble(key: String): Double? = settings.getStringOrNull(key)?.replace(',', '.')?.toDoubleOrNull()
    public fun putBoolean(key: String, value: Boolean): Unit = settings.putBoolean(key, value)
    public fun getBoolean(key: String): Boolean? = settings.getBooleanOrNull(key)
    public fun putString(key: String, value: String): Unit = settings.putString(key, value)
    public fun getString(key: String): String? = settings.getStringOrNull(key)

    public fun putCoordinate(key: String, value: Coordinate): Unit = putString(key, "${value.latitude},${value.longitude}")
    public fun getCoordinate(key: String): Coordinate? {
        val parts = getString(key)?.split(',') ?: return null
        if (parts.size != 2) return null
        val latitude = parts[0].toDoubleOrNull() ?: return null
        val longitude = parts[1].toDoubleOrNull() ?: return null
        return Coordinate(latitude, longitude)
    }
    public fun putLocalDate(key: String, value: LocalDate): Unit = putString(key, value.toString())
    public fun getLocalDate(key: String): LocalDate? = getString(key)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    public fun putInstant(key: String, value: Instant): Unit = putLong(key, value.toEpochMilliseconds())
    public fun getInstant(key: String): Instant? = getLong(key)?.let(Instant::fromEpochMilliseconds)
    public fun putDuration(key: String, value: Duration): Unit = putLong(key, value.inWholeMilliseconds)
    public fun getDuration(key: String): Duration? = getLong(key)?.milliseconds
}

public fun Preferences.putIntArray(key: String, value: List<Int>): Unit = putString(key, value.joinToString(","))
public fun Preferences.getIntArray(key: String): List<Int>? = getString(key)?.split(",")?.mapNotNull(String::toIntOrNull)
public fun Preferences.putLongArray(key: String, value: List<Long>): Unit = putString(key, value.joinToString(","))
public fun Preferences.getLongArray(key: String): List<Long>? = getString(key)?.split(",")?.mapNotNull(String::toLongOrNull)

public fun Preferences.putOrRemoveFloat(key: String, value: Float?) {
    if (value == null) remove(key) else putFloat(key, value)
}
public fun Preferences.putOrRemoveCoordinate(key: String, value: Coordinate?) {
    if (value == null) remove(key) else putCoordinate(key, value)
}
