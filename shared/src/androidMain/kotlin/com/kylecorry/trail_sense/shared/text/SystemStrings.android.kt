package com.kylecorry.trail_sense.shared.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
actual fun systemString(string: SystemString): String {
    return stringResource(
        when (string) {
            SystemString.Ok -> android.R.string.ok
            SystemString.Cancel -> android.R.string.cancel
            SystemString.Copy -> android.R.string.copy
            SystemString.Untitled -> android.R.string.untitled
        }
    )
}
