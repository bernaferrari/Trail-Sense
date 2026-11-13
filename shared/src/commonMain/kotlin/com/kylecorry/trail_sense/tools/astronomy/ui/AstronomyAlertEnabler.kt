package com.kylecorry.trail_sense.tools.astronomy.ui

import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import kotlinx.coroutines.CoroutineScope

/**
 * Turns on a sun alert service ([serviceId]) and asks, through [effects] when there is a screen, for what the platform
 * needs to deliver it on time (exact alarms, background location). Supplied by the platform.
 */
fun interface AstronomyAlertEnabler {
    suspend fun enable(serviceId: String, effects: ScreenEffects?, scope: CoroutineScope)
}
