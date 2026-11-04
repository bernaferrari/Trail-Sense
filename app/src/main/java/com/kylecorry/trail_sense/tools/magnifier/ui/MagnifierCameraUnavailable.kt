package com.kylecorry.trail_sense.tools.magnifier.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState

/**
 * The magnifier without camera access: the calm empty state with a plain camera (what the button gives back, not a
 * broken camera). Its one action asks for the permission again while Android still allows it, and opens the app's
 * settings once the answer is permanent.
 */
@Composable
internal fun MagnifierCameraUnavailable(
    onRequestCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    TrailSenseEmptyState(
        icon = Icons.Rounded.PhotoCamera,
        title = stringResource(R.string.magnifier_camera_needed_title),
        description = stringResource(R.string.magnifier_camera_needed_description),
        actionLabel = stringResource(R.string.magnifier_allow_camera),
        onAction = {
            val canAskAgain = context.findActivity()
                ?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == true
            if (canAskAgain) {
                onRequestCamera()
            } else {
                tryOrNothing { context.startActivity(Intents.appSettings(context)) }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
