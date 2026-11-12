package com.kylecorry.trail_sense.shared.alerts

import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.TrailSenseDialog
import com.kylecorry.trail_sense.shared.views.compose.AppHost

class AlertLoadingIndicator(private val title: String) :
    ILoadingIndicator {

    private var dialog: TrailSenseDialog? = null

    override fun show() {
        hide()
        dialog = Alerts.loading(AppHost.current(), title)
    }

    override fun hide() {
        dialog?.dismiss()
        dialog = null
    }
}
