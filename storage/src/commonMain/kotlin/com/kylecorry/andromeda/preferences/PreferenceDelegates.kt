package com.kylecorry.andromeda.preferences

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Creates a typed property delegate backed by this preferences store. */
public fun <T> Preferences.value(
    key: String,
    default: T,
    read: Preferences.(String) -> T?,
    write: Preferences.(String, T) -> Unit,
    saveDefault: Boolean = false
): ReadWriteProperty<Any?, T> = object : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val result = read(key)
        if (result == null && saveDefault) write(key, default)
        return result ?: default
    }
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = write(key, value)
}

public fun Preferences.string(key: String, default: String = "", saveDefault: Boolean = false): ReadWriteProperty<Any?, String> =
    value(key, default, Preferences::getString, Preferences::putString, saveDefault)
public fun Preferences.int(key: String, default: Int = 0, saveDefault: Boolean = false): ReadWriteProperty<Any?, Int> =
    value(key, default, Preferences::getInt, Preferences::putInt, saveDefault)
public fun Preferences.long(key: String, default: Long = 0, saveDefault: Boolean = false): ReadWriteProperty<Any?, Long> =
    value(key, default, Preferences::getLong, Preferences::putLong, saveDefault)
public fun Preferences.float(key: String, default: Float = 0f, saveDefault: Boolean = false): ReadWriteProperty<Any?, Float> =
    value(key, default, Preferences::getFloat, Preferences::putFloat, saveDefault)
public fun Preferences.double(key: String, default: Double = 0.0, saveDefault: Boolean = false): ReadWriteProperty<Any?, Double> =
    value(key, default, Preferences::getDouble, Preferences::putDouble, saveDefault)
public fun Preferences.boolean(key: String, default: Boolean = false, saveDefault: Boolean = false): ReadWriteProperty<Any?, Boolean> =
    value(key, default, Preferences::getBoolean, Preferences::putBoolean, saveDefault)

public fun Preferences.coordinate(key: String, default: com.kylecorry.sol.units.Coordinate, saveDefault: Boolean = false): ReadWriteProperty<Any?, com.kylecorry.sol.units.Coordinate> =
    value(key, default, Preferences::getCoordinate, Preferences::putCoordinate, saveDefault)
public fun Preferences.localDate(key: String, default: kotlinx.datetime.LocalDate, saveDefault: Boolean = false): ReadWriteProperty<Any?, kotlinx.datetime.LocalDate> =
    value(key, default, Preferences::getLocalDate, Preferences::putLocalDate, saveDefault)
public fun Preferences.instant(key: String, default: kotlin.time.Instant, saveDefault: Boolean = false): ReadWriteProperty<Any?, kotlin.time.Instant> =
    value(key, default, Preferences::getInstant, Preferences::putInstant, saveDefault)
public fun Preferences.duration(key: String, default: kotlin.time.Duration, saveDefault: Boolean = false): ReadWriteProperty<Any?, kotlin.time.Duration> =
    value(key, default, Preferences::getDuration, Preferences::putDuration, saveDefault)
public inline fun <reified T : Enum<T>> Preferences.enum(key: String, default: T, saveDefault: Boolean = false): ReadWriteProperty<Any?, T> =
    value(key, default, { name -> getString(name)?.let { value -> enumValues<T>().find { it.name == value } } },
        { name, value -> putString(name, value.name) }, saveDefault)

/** Persists a value using an explicit integer mapping, suitable for stable serialized IDs. */
public fun <T : Any> Preferences.intMapping(key: String, default: T, mappings: Map<Int, T>, saveDefault: Boolean = false): ReadWriteProperty<Any?, T> =
    mappedValue(key, default, mappings, Preferences::getInt, Preferences::putInt, saveDefault)

/** Persists a value using an explicit string mapping. */
public fun <T : Any> Preferences.stringMapping(key: String, default: T, mappings: Map<String, T>, saveDefault: Boolean = false): ReadWriteProperty<Any?, T> =
    mappedValue(key, default, mappings, Preferences::getString, Preferences::putString, saveDefault)

private fun <K : Any, T : Any> Preferences.mappedValue(
    key: String,
    default: T,
    mappings: Map<K, T>,
    read: Preferences.(String) -> K?,
    write: Preferences.(String, K) -> Unit,
    saveDefault: Boolean,
): ReadWriteProperty<Any?, T> = object : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val stored = read(key)
        if (stored == null && saveDefault) mappings.entries.firstOrNull { it.value == default }?.key?.let { write(key, it) }
        return mappings[stored] ?: default
    }
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        mappings.entries.firstOrNull { it.value == value }?.key?.let { write(key, it) }
    }
}
