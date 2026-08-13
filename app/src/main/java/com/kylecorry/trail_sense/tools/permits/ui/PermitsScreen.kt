@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.permits.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState

@Composable
fun PermitsScreen(
    modifier: Modifier = Modifier
) {
    TrailSenseEmptyState(
        icon = Icons.Default.Info,
        title = stringResource(R.string.no_permits),
        modifier = modifier.fillMaxSize()
    )
}
