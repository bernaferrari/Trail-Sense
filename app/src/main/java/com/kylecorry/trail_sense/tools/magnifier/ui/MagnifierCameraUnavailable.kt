package com.kylecorry.trail_sense.tools.magnifier.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NoPhotography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState

/** The magnifier without a camera: the calm empty state, whose one action opens the app's settings. */
@Composable
internal fun MagnifierCameraUnavailable(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    TrailSenseEmptyState(
        icon = Icons.Rounded.NoPhotography,
        title = stringResource(R.string.no_camera_access),
        description = stringResource(R.string.camera_unavailable_description),
        actionLabel = stringResource(R.string.open_settings),
        onAction = { tryOrNothing { context.startActivity(Intents.appSettings(context)) } },
        modifier = modifier.fillMaxSize()
    )
}
