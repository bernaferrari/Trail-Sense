package com.kylecorry.trail_sense.shared.sharing

import androidx.compose.runtime.staticCompositionLocalOf
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects

/** Offers a location to share (copy, QR code, maps, other apps) and sends it the chosen way. */
fun interface LocationSharer {
    suspend fun share(effects: ScreenEffects, navigator: TrailSenseNavigator, location: Coordinate, title: String)
}

/** The platform's location sharing, or null where there is none (screens then hide sharing). */
val LocalLocationSharer = staticCompositionLocalOf<LocationSharer?> { null }
