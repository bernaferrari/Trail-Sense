package com.kylecorry.trail_sense.shared.andromeda_temp.background.services

import android.app.Notification

data class ForegroundInfo(
    val id: Int,
    val notification: Notification,
    val foregroundServiceTypeOverride: List<Int>? = null
)