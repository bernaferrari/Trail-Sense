package com.kylecorry.andromeda.background

import android.os.Bundle

internal fun Bundle.toMap(): Map<String, Any?> {
    return keySet().associateWith {
        @Suppress("DEPRECATION")
        get(it)
    }
}
