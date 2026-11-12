package com.kylecorry.trail_sense.shared.text

import androidx.compose.runtime.Composable

/** Common labels the platform already translates, so the app doesn't ship its own copies. */
enum class SystemString {
    Ok,
    Cancel,
    Copy,
    Untitled
}

/** The platform's translation of [string] in the app's language. */
@Composable
expect fun systemString(string: SystemString): String
