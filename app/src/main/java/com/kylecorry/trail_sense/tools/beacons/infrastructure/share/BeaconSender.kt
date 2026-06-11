package com.kylecorry.trail_sense.tools.beacons.infrastructure.share

import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.sharing.Share
import com.kylecorry.trail_sense.shared.sharing.ShareAction
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.qr.infrastructure.BeaconQREncoder

class BeaconSender(private val host: TrailSenseDestinationHost) : IBeaconSender {
    override fun send(beacon: Beacon) {
        Share.share(
            host, beacon.name, listOf(
                ShareAction.Copy,
                ShareAction.QR,
                ShareAction.Maps,
                ShareAction.Send
            )
        ) {
            when (it) {
                ShareAction.Copy -> {
                    val sender = BeaconCopy(host.requireContext())
                    sender.send(beacon)
                }

                ShareAction.QR -> {
                    CustomUiUtils.showQR(
                        host,
                        beacon.name,
                        BeaconQREncoder().encode(beacon)
                    )
                }

                ShareAction.Maps -> {
                    val sender = BeaconGeoSender(host.requireContext())
                    sender.send(beacon)
                }

                ShareAction.Send -> {
                    val sender = BeaconSharesheet(host.requireContext())
                    sender.send(beacon)
                }

                else -> {
                    // Do nothing
                }
            }
        }
    }
}
