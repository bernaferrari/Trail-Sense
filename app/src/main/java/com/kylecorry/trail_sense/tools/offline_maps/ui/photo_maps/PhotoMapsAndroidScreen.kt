package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.from
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.print.Printer
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.commands.PrintMapCommand

/**
 * The photo map screen on Android: export through the activity's file picker, the system print dialog, and the
 * View-based warp, calibration and map steps.
 */
@Composable
fun PhotoMapsAndroidToolScreen(
    route: Route.PhotoMaps,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effects = LocalScreenEffects.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val exportService = remember(context, effects, lifecycleOwner) {
        // Exporting picks a file through the activity's result launcher (an Activity-only API).
        PhotoMapExportService(context, requireNotNull(context.findMainActivity()), effects, lifecycleOwner)
    }
    val canPrint = remember { Printer.canPrint() }
    PhotoMapsToolScreen(
        route = route,
        mapService = getAppService(),
        formatter = getAppService(),
        host = AppHost.from(context),
        exportMap = exportService::export,
        printMap = if (canPrint) {
            { PrintMapCommand(context).execute(it) }
        } else {
            null
        },
        child = { child -> PhotoMapStep(child) },
        modifier = modifier
    )
}

@Composable
private fun PhotoMapStep(child: PhotoMapChildHost) {
    when (val spec = child.spec) {
        is PhotoMapChildSpec.Warp -> WarpMapToolScreen(
            modifier = Modifier.fillMaxSize(),
            mapId = spec.mapId,
            viewModelKey = child.viewModelKey,
            onComplete = child.onWarpComplete
        )

        is PhotoMapChildSpec.Calibration -> PhotoMapCalibrationToolScreen(
            modifier = Modifier.fillMaxSize(),
            mapId = spec.mapId,
            viewModelKey = child.viewModelKey,
            commands = child.commands,
            onRotation = child.onCalibrationRotation,
            onComplete = child.onCalibrationComplete
        )

        is PhotoMapChildSpec.View -> ViewPhotoMapToolScreen(
            modifier = Modifier.fillMaxSize(),
            mapId = spec.mapId,
            viewModelKey = child.viewModelKey,
            autoLockLocation = spec.autoLockLocation,
            commands = child.commands
        )
    }
}

private tailrec fun Context.findMainActivity(): MainActivity? {
    return when (this) {
        is MainActivity -> this
        is ContextWrapper -> baseContext.findMainActivity()
        else -> null
    }
}
