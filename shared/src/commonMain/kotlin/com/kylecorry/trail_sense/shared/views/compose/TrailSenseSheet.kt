@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The app's one bottom sheet: a Material ModalBottomSheet on surfaceContainerLow that opens fully (no half state),
 * an optional titleLarge header, and its content 24 dp in from the sides and the bottom. Every sheet goes through
 * this; pass [contentPadding] only for edge-to-edge content (a camera preview, a list with its own gutters).
 */
@Composable
fun TrailSenseSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    contentPadding: PaddingValues = TrailSenseSheetDefaults.contentPadding,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(Modifier.fillMaxWidth().padding(contentPadding)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = TrailSenseSpacing.lg)
                )
            }
            content()
        }
    }
}

object TrailSenseSheetDefaults {
    /** 24 dp at the sides and the bottom; the drag handle already spaces the top. */
    val contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp)
}
