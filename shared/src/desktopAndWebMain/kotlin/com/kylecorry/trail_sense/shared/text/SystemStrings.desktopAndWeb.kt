package com.kylecorry.trail_sense.shared.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun systemString(string: SystemString): String {
    return remember(string) { portableSystemString(string) }
}

internal fun portableSystemString(string: SystemString): String = when (string) {
    SystemString.Ok -> "OK"
    SystemString.Cancel -> "Cancel"
    SystemString.Copy -> "Copy"
    SystemString.Untitled -> "Untitled"
}
