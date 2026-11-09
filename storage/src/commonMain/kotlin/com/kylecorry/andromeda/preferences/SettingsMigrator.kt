package com.kylecorry.andromeda.preferences

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A single version-to-version preferences migration. */
public data class SettingsMigration(val fromVersion: Int, val action: suspend Preferences.() -> Unit)

public class SettingsMigrator(private val preferences: Preferences, private val versionKey: String) {
    private val mutex = Mutex()

    /** Applies migrations in order; versions without a migration are advanced unchanged. */
    public suspend fun migrate(toVersion: Int, migrations: List<SettingsMigration>): Unit = mutex.withLock {
        require(toVersion >= 0)
        require(migrations.map { it.fromVersion }.distinct().size == migrations.size)
        var current = preferences.getInt(versionKey) ?: 0
        while (current < toVersion) {
            migrations.firstOrNull { it.fromVersion == current }?.action?.invoke(preferences)
            current++
            preferences.putInt(versionKey, current)
        }
    }
}
