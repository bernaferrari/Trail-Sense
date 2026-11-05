package com.kylecorry.trail_sense.tools.triangulate.ui

import android.content.Context
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.sharing.ActionItem
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import com.kylecorry.trail_sense.tools.navigation.infrastructure.share.LocationCopy
import com.kylecorry.trail_sense.tools.navigation.infrastructure.share.LocationGeoSender
import com.kylecorry.trail_sense.tools.navigation.infrastructure.share.LocationQRSender
import com.kylecorry.trail_sense.tools.navigation.infrastructure.share.LocationSharesheet

/**
 * Offers [location] as Copy, QR code, Maps and Send in the action sheet, like the shared Share.shareLocation, but
 * through [ScreenEffects] instead of the destination host.
 */
internal suspend fun shareLocation(
    context: Context,
    navigator: TrailSenseNavigator,
    effects: ScreenEffects,
    location: Coordinate
) {
    val format = UserPreferences(context).coordinateFormat
    val actions = listOf(
        ActionItem(context.getString(android.R.string.copy), TrailIcons.Copy) {
            LocationCopy(context).send(location, format)
        },
        ActionItem(context.getString(R.string.qr_code), TrailIcons.QrCode) {
            LocationQRSender(context, navigator).send(location, format)
        },
        ActionItem(context.getString(R.string.maps), TrailIcons.Maps) {
            LocationGeoSender(context).send(location, format)
        },
        ActionItem(context.getString(R.string.share_action_send), TrailIcons.Send) {
            LocationSharesheet(context).send(location, format)
        }
    )
    effects.chooseAction(context.getString(R.string.location), actions = actions)?.action?.invoke()
}
