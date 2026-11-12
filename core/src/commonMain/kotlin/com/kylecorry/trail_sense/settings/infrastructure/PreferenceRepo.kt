package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.andromeda.preferences.Preferences

/** A group of related settings stored in [cache]. */
abstract class PreferenceRepo(protected val cache: Preferences)
