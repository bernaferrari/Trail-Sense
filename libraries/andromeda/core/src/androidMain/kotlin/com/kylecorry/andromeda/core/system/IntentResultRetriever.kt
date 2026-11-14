package com.kylecorry.andromeda.core.system

import android.content.Intent

public interface IntentResultRetriever {
    public fun getResult(intent: Intent, action: (successful: Boolean, data: Intent?) -> Unit)
}