package com.kylecorry.andromeda.preferences

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SettingsListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private fun <T> Preferences.observe(
    key: String,
    read: Preferences.(String) -> T,
    register: ObservableSettings.(String, (T) -> Unit) -> SettingsListener
): Flow<T> = callbackFlow {
    val observable = settings as? ObservableSettings
        ?: error("The supplied Settings implementation does not support observation")
    val listener = observable.register(key) { trySend(it) }
    trySend(read(key))
    awaitClose { listener.deactivate() }
}

public fun Preferences.observeInt(key: String): Flow<Int?> =
    observe(key, Preferences::getInt) { name, callback -> addIntOrNullListener(name, callback) }
public fun Preferences.observeLong(key: String): Flow<Long?> =
    observe(key, Preferences::getLong) { name, callback -> addLongOrNullListener(name, callback) }
public fun Preferences.observeFloat(key: String): Flow<Float?> =
    observe(key, Preferences::getFloat) { name, callback -> addFloatOrNullListener(name, callback) }
public fun Preferences.observeBoolean(key: String): Flow<Boolean?> =
    observe(key, Preferences::getBoolean) { name, callback -> addBooleanOrNullListener(name, callback) }
public fun Preferences.observeString(key: String): Flow<String?> =
    observe(key, Preferences::getString) { name, callback -> addStringOrNullListener(name, callback) }
public fun Preferences.observeDouble(key: String): Flow<Double?> =
    observe(key, Preferences::getDouble) { name, callback ->
        addStringOrNullListener(name) { callback(it?.replace(',', '.')?.toDoubleOrNull()) }
    }
