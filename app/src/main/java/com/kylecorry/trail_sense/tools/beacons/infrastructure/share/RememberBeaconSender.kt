package com.kylecorry.trail_sense.tools.beacons.infrastructure.share

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon

/** Shares a beacon with the Android share sheet, maps apps, the clipboard, or a QR code. */
@Composable
fun rememberBeaconShare(): (Beacon) -> Unit {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    return remember(context, navigator, effects, scope) {
        { beacon -> BeaconSender(context, effects, navigator, scope).send(beacon) }
    }
}
