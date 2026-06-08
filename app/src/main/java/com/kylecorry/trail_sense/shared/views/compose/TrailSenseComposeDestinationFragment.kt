package com.kylecorry.trail_sense.shared.views.compose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.trail_sense.tools.WhistleScreen
import com.kylecorry.trail_sense.tools.level.ui.LevelScreen

class TrailSenseComposeDestinationFragment : TrailSenseComposeFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when (requireArguments().getString(ARG_DESTINATION)) {
            DESTINATION_LOCAL_MESSAGING -> openApp(
                requireContext(),
                "com.kylecorry.trail_sense_comms",
                "4c285dfe-1c8b-45eb-bb79-3f1d2eb6ae48"
            )
            DESTINATION_LOCAL_TALK -> openApp(
                requireContext(),
                "com.kylecorry.trail_sense_comms",
                "b76f32bf-6a72-4992-a741-0c9bf19ebd11"
            )
        }
    }

    @Composable
    override fun FragmentContent() {
        when (requireArguments().getString(ARG_DESTINATION)) {
            DESTINATION_LEVEL -> LevelScreen()
            DESTINATION_WHISTLE -> WhistleScreen()
        }
    }

    private fun openApp(context: Context, packageName: String, tool: String) {
        if (!Package.isPackageInstalled(context, packageName)) return
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.putExtras(Bundle().apply {
            putString("tool", tool)
        })
        context.startActivity(intent)
    }

    companion object {
        const val ARG_DESTINATION = "compose_destination"
        const val DESTINATION_LEVEL = "level"
        const val DESTINATION_WHISTLE = "whistle"
        const val DESTINATION_LOCAL_MESSAGING = "local_messaging"
        const val DESTINATION_LOCAL_TALK = "local_talk"
    }
}
