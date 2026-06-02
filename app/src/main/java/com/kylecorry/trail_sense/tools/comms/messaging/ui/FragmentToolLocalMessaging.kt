package com.kylecorry.trail_sense.tools.comms.messaging.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme

class FragmentToolLocalMessaging : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO: Only do this once
        openApp(
            requireContext(),
            "com.kylecorry.trail_sense_comms",
            "4c285dfe-1c8b-45eb-bb79-3f1d2eb6ae48"
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    Text(
                        text = "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
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
}
