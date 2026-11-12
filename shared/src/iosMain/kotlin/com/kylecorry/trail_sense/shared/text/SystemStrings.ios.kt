package com.kylecorry.trail_sense.shared.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSBundle

@Composable
actual fun systemString(string: SystemString): String {
    return remember(string) { iosSystemString(string) }
}

/**
 * UIKit's own translations of its standard button titles. They follow the languages the app is localized in;
 * the English text is the fallback when UIKit has no entry.
 */
internal fun iosSystemString(string: SystemString): String {
    val english = when (string) {
        SystemString.Ok -> "OK"
        SystemString.Cancel -> "Cancel"
        SystemString.Copy -> "Copy"
        SystemString.Untitled -> "Untitled"
    }
    val uiKit = NSBundle.bundleWithIdentifier("com.apple.UIKit") ?: return english
    return uiKit.localizedStringForKey(english, english, null)
}
