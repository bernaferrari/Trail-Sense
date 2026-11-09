package com.kylecorry.trail_sense.settings.migrations

import com.kylecorry.andromeda.preferences.Preferences
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Preferences backed by an in-memory map. Like the platform-backed store, every write is reported
 * through [Preferences.observeChanges].
 */
@Suppress("FunctionName")
fun InMemoryPreferences(): Preferences {
    val settings = NotifyingSettings(MapSettings())
    return Preferences(settings) { onChange ->
        settings.listeners.add(onChange)
        return@Preferences { settings.listeners.remove(onChange) }
    }
}

private class NotifyingSettings(private val delegate: Settings) : Settings by delegate {
    val listeners = CopyOnWriteArrayList<(String) -> Unit>()

    private fun changed(key: String) = listeners.forEach { it(key) }

    override fun clear() {
        val keys = delegate.keys
        delegate.clear()
        keys.forEach(::changed)
    }

    override fun remove(key: String) {
        delegate.remove(key)
        changed(key)
    }

    override fun putInt(key: String, value: Int) {
        delegate.putInt(key, value)
        changed(key)
    }

    override fun putLong(key: String, value: Long) {
        delegate.putLong(key, value)
        changed(key)
    }

    override fun putString(key: String, value: String) {
        delegate.putString(key, value)
        changed(key)
    }

    override fun putFloat(key: String, value: Float) {
        delegate.putFloat(key, value)
        changed(key)
    }

    override fun putDouble(key: String, value: Double) {
        delegate.putDouble(key, value)
        changed(key)
    }

    override fun putBoolean(key: String, value: Boolean) {
        delegate.putBoolean(key, value)
        changed(key)
    }
}
