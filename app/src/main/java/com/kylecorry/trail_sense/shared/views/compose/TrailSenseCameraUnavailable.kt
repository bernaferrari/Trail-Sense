package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NoPhotography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.trail_sense.R

/**
 * What a camera instrument shows instead of the preview when it can't use the camera: the kit's empty state, with
 * one way to the app's settings.
 */
@Composable
fun TrailSenseCameraUnavailable(
    modifier: Modifier = Modifier,
    description: String = stringResource(R.string.camera_unavailable_description)
) {
    val context = LocalContext.current
    TrailSenseEmptyState(
        icon = Icons.Rounded.NoPhotography,
        title = stringResource(R.string.no_camera_access),
        description = description,
        actionLabel = stringResource(R.string.open_settings),
        onAction = {
            tryOrNothing {
                context.startActivity(Intents.appSettings(context))
            }
        },
        modifier = modifier
    )
}
