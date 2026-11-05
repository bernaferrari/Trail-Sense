package com.kylecorry.trail_sense.tools.beacons.infrastructure.share

import android.content.Context
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.sharing.ShareAction
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.qr.infrastructure.BeaconQREncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Offers the share sheet for a beacon (copy, QR, maps, send) and sends it the chosen way. */
class BeaconSender(
    private val context: Context,
    private val effects: ScreenEffects,
    private val navigator: TrailSenseNavigator,
    private val scope: CoroutineScope
) : IBeaconSender {
    override fun send(beacon: Beacon) {
        scope.launch {
            val action = chooseShareAction(
                context,
                effects,
                beacon.name,
                listOf(ShareAction.Copy, ShareAction.QR, ShareAction.Maps, ShareAction.Send)
            )
            when (action) {
                ShareAction.Copy -> BeaconCopy(context).send(beacon)
                ShareAction.QR -> CustomUiUtils.showQR(
                    context,
                    navigator,
                    beacon.name,
                    BeaconQREncoder().encode(beacon)
                )

                ShareAction.Maps -> BeaconGeoSender(context).send(beacon)
                ShareAction.Send -> BeaconSharesheet(context).send(beacon)
                else -> {
                    // Do nothing
                }
            }
        }
    }
}
