package com.kylecorry.trail_sense.shared.sharing

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat
import com.kylecorry.trail_sense.tools.navigation.infrastructure.share.LocationCopy
import com.kylecorry.trail_sense.tools.navigation.infrastructure.share.LocationGeoSender
import com.kylecorry.trail_sense.tools.navigation.infrastructure.share.LocationQRSender
import com.kylecorry.trail_sense.tools.navigation.infrastructure.share.LocationSharesheet

object Share {

    fun shareLocation(
        host: TrailSenseDestinationHost,
        location: Coordinate,
        format: BuiltInCoordinateFormat = UserPreferences(host.requireContext()).coordinateFormat,
        title: String = host.getString(R.string.location)
    ) {
        val locationSenders = mapOf(
            ShareAction.Copy to LocationCopy(host.requireContext()),
            ShareAction.QR to LocationQRSender(host.requireContext(), host.findNavController()),
            ShareAction.Maps to LocationGeoSender(host.requireContext()),
            ShareAction.Send to LocationSharesheet(host.requireContext())
        )

        share(
            host,
            title,
            listOf(ShareAction.Copy, ShareAction.QR, ShareAction.Send, ShareAction.Maps)
        ) {
            it?.let {
                locationSenders[it]?.send(location, format)
            }
        }
    }

    fun share(
        host: TrailSenseDestinationHost,
        title: String,
        actions: List<ShareAction>,
        subtitle: String? = null,
        onAction: (action: ShareAction?) -> Unit
    ) {

        val titles = mapOf(
            ShareAction.Copy to host.getString(androidx.preference.R.string.copy),
            ShareAction.QR to host.getString(R.string.qr_code),
            ShareAction.Send to host.getString(R.string.share_action_send),
            ShareAction.Maps to host.getString(R.string.maps),
            ShareAction.File to host.getString(R.string.file),
            ShareAction.Navigate to host.getString(R.string.navigate),
            ShareAction.Open to host.getString(R.string.open)
        )

        val icons = mapOf(
            ShareAction.Copy to TrailIcons.Copy,
            ShareAction.QR to TrailIcons.QrCode,
            ShareAction.Send to TrailIcons.Send,
            ShareAction.Maps to TrailIcons.Maps,
            ShareAction.File to TrailIcons.File,
            ShareAction.Navigate to TrailIcons.Beacon,
            ShareAction.Open to TrailIcons.Open
        )

        val actionItems = actions.sortedBy { it.ordinal }.map {
            ActionItem(titles[it] ?: "", icons[it] ?: TrailIcons.Send) {
                onAction(it)
            }
        }

        actions(host, title, actionItems, subtitle = subtitle) {
            onAction(null)
        }
    }

    fun actions(
        host: TrailSenseDestinationHost,
        title: String,
        actions: List<ActionItem>,
        subtitle: String? = null,
        noActionSelected: () -> Unit
    ) {
        var called = false

        val customOnAction = { action: ActionItem? ->
            if (!called) {
                called = true
                if (action != null) {
                    action.action()
                } else {
                    noActionSelected()
                }
            }
        }

        host.showActionSheet(title, subtitle, actions, customOnAction)
    }

}

enum class ShareAction {
    Copy,
    QR,
    Maps,
    Send,
    File,
    Navigate,
    Open
}
