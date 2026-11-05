package com.kylecorry.trail_sense.tools.navigation.infrastructure.share

import android.content.Context
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.sharing.ShareAction
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import com.kylecorry.trail_sense.tools.beacons.infrastructure.share.chooseShareAction

/** Offers the share sheet for a location (copy, QR, send, maps) and sends it the chosen way; Share.shareLocation without a host. */
internal suspend fun shareLocation(
    context: Context,
    effects: ScreenEffects,
    navigator: TrailSenseNavigator,
    location: Coordinate,
    format: BuiltInCoordinateFormat = UserPreferences(context).coordinateFormat,
    title: String = context.getString(R.string.location)
) {
    val senders = mapOf(
        ShareAction.Copy to LocationCopy(context),
        ShareAction.QR to LocationQRSender(context, navigator),
        ShareAction.Maps to LocationGeoSender(context),
        ShareAction.Send to LocationSharesheet(context)
    )
    val action = chooseShareAction(
        context,
        effects,
        title,
        listOf(ShareAction.Copy, ShareAction.QR, ShareAction.Send, ShareAction.Maps)
    ) ?: return
    senders[action]?.send(location, format)
}
