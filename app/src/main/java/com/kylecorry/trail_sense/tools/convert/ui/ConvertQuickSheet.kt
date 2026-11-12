package com.kylecorry.trail_sense.tools.convert.ui

import android.content.Context
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.AppOverlay
import com.kylecorry.trail_sense.shared.views.compose.from

/**
 * The convert quick action: the converter in a sheet over the current screen, as upstream's convert bottom sheet was,
 * so a quick conversion doesn't leave the screen you are on.
 */
fun showConvertQuickSheet(context: Context) {
    var overlay: AppOverlay? = null
    overlay = AppHost.from(context).addOverlay {
        ConvertQuickSheet(onDismiss = { overlay?.remove() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConvertQuickSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        )
    ) {
        ConvertScreen(
            modifier = Modifier
                // The sheet is its own window, so it repeats the app root's test tag setting for UiAutomator
                .semantics { testTagsAsResourceId = true }
                .fillMaxWidth()
                .fillMaxHeight(SHEET_HEIGHT_FRACTION)
        )
    }
}

private const val SHEET_HEIGHT_FRACTION = 0.9f
