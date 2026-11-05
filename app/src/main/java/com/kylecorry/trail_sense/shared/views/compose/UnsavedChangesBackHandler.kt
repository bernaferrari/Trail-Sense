package com.kylecorry.trail_sense.shared.views.compose

import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator

/**
 * Asks before back leaves a form with unsaved changes. While [hasChanges] is true, back (the gesture, the button or
 * the top bar's arrow) opens the family's dialog instead of leaving; Leave pops the screen.
 */
@Composable
fun UnsavedChangesBackHandler(hasChanges: Boolean) {
    val navigator = LocalNavigator.current
    var asking by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = hasChanges) { asking = true }
    if (asking) {
        TrailSenseDialog(
            onDismissRequest = { asking = false },
            title = stringResource(R.string.unsaved_changes),
            confirmText = stringResource(R.string.dialog_leave),
            onConfirm = {
                asking = false
                navigator.popBackStack()
            },
            dismissText = stringResource(android.R.string.cancel)
        ) {
            Text(stringResource(R.string.unsaved_changes_message))
        }
    }
}
