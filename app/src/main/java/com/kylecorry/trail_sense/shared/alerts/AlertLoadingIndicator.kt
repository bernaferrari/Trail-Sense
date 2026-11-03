package com.kylecorry.trail_sense.shared.alerts

import android.content.Context
import android.app.Dialog
import com.kylecorry.trail_sense.shared.dialogs.Alerts

class AlertLoadingIndicator(private val context: Context, private val title: String) :
    ILoadingIndicator {

    private var dialog: Dialog? = null

    override fun show() {
        hide()
        dialog = Alerts.loading(context, title)
    }

    override fun hide() {
        dialog?.dismiss()
        dialog = null
    }
}
