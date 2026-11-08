package com.kylecorry.andromeda.preferences

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Creates a typed property delegate backed by this preferences store. */
fun <T> Preferences.value(
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

fun Preferences.string(key: String, default: String = "", saveDefault: Boolean = false) =
    value(key, default, Preferences::getString, Preferences::putString, saveDefault)
fun Preferences.int(key: String, default: Int = 0, saveDefault: Boolean = false) =
    value(key, default, Preferences::getInt, Preferences::putInt, saveDefault)
fun Preferences.long(key: String, default: Long = 0, saveDefault: Boolean = false) =
    value(key, default, Preferences::getLong, Preferences::putLong, saveDefault)
fun Preferences.float(key: String, default: Float = 0f, saveDefault: Boolean = false) =
    value(key, default, Preferences::getFloat, Preferences::putFloat, saveDefault)
fun Preferences.double(key: String, default: Double = 0.0, saveDefault: Boolean = false) =
    value(key, default, Preferences::getDouble, Preferences::putDouble, saveDefault)
fun Preferences.boolean(key: String, default: Boolean = false, saveDefault: Boolean = false) =
    value(key, default, Preferences::getBoolean, Preferences::putBoolean, saveDefault)

fun Preferences.coordinate(key: String, default: com.kylecorry.sol.units.Coordinate, saveDefault: Boolean = false) =
    value(key, default, Preferences::getCoordinate, Preferences::putCoordinate, saveDefault)
fun Preferences.localDate(key: String, default: kotlinx.datetime.LocalDate, saveDefault: Boolean = false) =
    value(key, default, Preferences::getLocalDate, Preferences::putLocalDate, saveDefault)
fun Preferences.instant(key: String, default: kotlin.time.Instant, saveDefault: Boolean = false) =
    value(key, default, Preferences::getInstant, Preferences::putInstant, saveDefault)
fun Preferences.duration(key: String, default: kotlin.time.Duration, saveDefault: Boolean = false) =
    value(key, default, Preferences::getDuration, Preferences::putDuration, saveDefault)
inline fun <reified T : Enum<T>> Preferences.enum(key: String, default: T, saveDefault: Boolean = false) =
    value(key, default, { name -> getString(name)?.let { value -> enumValues<T>().find { it.name == value } } },
        { name, value -> putString(name, value.name) }, saveDefault)
