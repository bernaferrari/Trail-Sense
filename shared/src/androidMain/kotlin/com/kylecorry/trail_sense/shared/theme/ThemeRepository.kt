package com.kylecorry.trail_sense.shared.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kylecorry.trail_sense.shared.views.TrailSenseColorScheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Modern DataStore-backed repository for the app's color theme choice.
 *
 * This is the single source of truth for which TrailSenseColorScheme (Orange, Blue, ..., Dynamic)
 * the user has selected. It replaces / augments the old boolean "use dynamic colors" for the new
 * expressive theming system.
 *
 * Persisted using Android DataStore (coroutine-friendly, safe, migratable to KMP later).
 */
class ThemeRepository private constructor(private val context: Context) {

    private val dataStore: DataStore<Preferences> = context.applicationContext.themeDataStore

    /**
     * The current color scheme chosen by the user, as a Flow.
     * Defaults to Orange on first launch / if nothing stored yet.
     */
    val colorScheme: Flow<TrailSenseColorScheme> = dataStore.data
        .map { preferences ->
            val id = preferences[KEY_COLOR_SCHEME_ID] ?: TrailSenseColorScheme.toId(TrailSenseColorScheme.Orange)
            TrailSenseColorScheme.fromId(id)
        }

    /**
     * Persist a new color scheme choice (one of the 6 presets or Dynamic).
     */
    suspend fun setColorScheme(scheme: TrailSenseColorScheme) {
        dataStore.edit { prefs ->
            prefs[KEY_COLOR_SCHEME_ID] = TrailSenseColorScheme.toId(scheme)
        }
    }

    companion object {
        private val Context.themeDataStore by preferencesDataStore(
            name = "trail_sense_theme"
        )

        private val KEY_COLOR_SCHEME_ID = intPreferencesKey("color_scheme_id")

        @Volatile
        private var INSTANCE: ThemeRepository? = null

        /**
         * Returns the process-wide singleton repository.
         * Safe to call from any Context (it will use applicationContext internally).
         */
        fun getInstance(context: Context): ThemeRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThemeRepository(context).also { INSTANCE = it }
            }
        }
    }
}
