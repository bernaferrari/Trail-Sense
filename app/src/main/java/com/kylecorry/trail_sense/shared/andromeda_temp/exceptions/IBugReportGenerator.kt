package com.kylecorry.trail_sense.shared.andromeda_temp.exceptions

import android.content.Context

interface IBugReportGenerator {
    fun generate(context: Context, throwable: Throwable): String
}