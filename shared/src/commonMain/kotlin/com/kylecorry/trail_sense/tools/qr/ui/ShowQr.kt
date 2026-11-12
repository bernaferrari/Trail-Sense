package com.kylecorry.trail_sense.tools.qr.ui

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.qr_text_too_long
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost

/** The most text a shared QR code holds; longer text is cut off. */
const val QR_SHARE_MAX_LENGTH = 1000

/** Opens [qr] as a QR code titled [title], warning first when it is too long to fit. */
fun showQR(host: AppHost, navigator: TrailSenseNavigator, title: String, qr: String) {
    if (qr.length > QR_SHARE_MAX_LENGTH) {
        Alerts.snackbar(host, getAppService<AppStrings>().get(Res.string.qr_text_too_long))
    }
    navigator.navigate(Route.ViewQr(title = title, text = qr.take(QR_SHARE_MAX_LENGTH)))
}
