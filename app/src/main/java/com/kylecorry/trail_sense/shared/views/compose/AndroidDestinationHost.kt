package com.kylecorry.trail_sense.shared.views.compose

import android.content.Context

/** A destination host on Android, with the activity context its quick actions and dialogs show from. */
interface AndroidDestinationHost : TrailSenseDestinationHost {
    val context: Context
}
