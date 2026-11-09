package com.kylecorry.trail_sense.shared.andromeda_temp.core.system

import android.content.Intent

interface IntentResultRetriever {
    fun getResult(intent: Intent, action: (successful: Boolean, data: Intent?) -> Unit)
}