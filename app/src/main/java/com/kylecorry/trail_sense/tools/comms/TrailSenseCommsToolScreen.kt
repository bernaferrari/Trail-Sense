package com.kylecorry.trail_sense.tools.comms

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost

/**
 * Local Messaging and Local Talk live in the Trail Sense Comms app: opening the tool opens that app on [tool] (when it
 * is installed) and draws nothing here.
 */
@Composable
fun TrailSenseCommsToolScreen(host: TrailSenseDestinationHost, tool: String) {
    LaunchedEffect(host, tool) {
        openCommsApp(host.requireContext(), tool)
    }
}

private const val COMMS_PACKAGE = "com.kylecorry.trail_sense_comms"

private fun openCommsApp(context: Context, tool: String) {
    if (!Package.isPackageInstalled(context, COMMS_PACKAGE)) return
    val intent = context.packageManager.getLaunchIntentForPackage(COMMS_PACKAGE) ?: return
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    intent.putExtras(Bundle().apply {
        putString("tool", tool)
    })
    context.startActivity(intent)
}
