package com.kylecorry.trail_sense.tools.augmented_reality.ui

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.alerts.IAlerter
import com.kylecorry.trail_sense.shared.alerts.MissingSensorAlert

class ARNoGyroAlert(private val context: Context) : IAlerter {
    override fun alert() = alert {}

    /** Shows the disclaimer when it hasn't been agreed to; [onClose] runs once it is closed, or right away. */
    fun alert(onClose: () -> Unit) {
        CustomUiUtils.disclaimer(
            context,
            MissingSensorAlert.getMissingSensorTitle(
                context,
                context.getString(R.string.sensor_gyroscope)
            ),
            context.getString(R.string.augmented_reality_no_gyro_message),
            "pref_ar_no_gyro_disclaimer",
            cancelText = null,
            onClose = { _, _ -> onClose() }
        )
    }
}
