package com.kylecorry.trail_sense.shared.views.compose

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.systemString
import com.kylecorry.trail_sense.shared.text.SystemString
import com.kylecorry.trail_sense.shared.text.appString

import com.kylecorry.trail_sense.shared.navigation.SystemBackHandler
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator

/**
 * Asks before back leaves a form with unsaved changes. While [hasChanges] is true, back (the gesture, the button or
 * the top bar's arrow) opens the family's dialog instead of leaving; Leave pops the screen.
 */
@Composable
fun UnsavedChangesBackHandler(hasChanges: Boolean) {
    val navigator = LocalNavigator.current
    var asking by rememberSaveable { mutableStateOf(false) }
    SystemBackHandler(enabled = hasChanges) { asking = true }
    if (asking) {
        TrailSenseDialog(
            onDismissRequest = { asking = false },
            title = appString(Res.string.unsaved_changes),
            confirmText = appString(Res.string.dialog_leave),
            onConfirm = {
                asking = false
                navigator.popBackStack()
            },
            dismissText = systemString(SystemString.Cancel)
        ) {
            Text(appString(Res.string.unsaved_changes_message))
        }
    }
}
