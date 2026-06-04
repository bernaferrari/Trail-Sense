package com.kylecorry.trail_sense.tools.comms.talk.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.kylecorry.andromeda.core.system.Package
import androidx.compose.runtime.Composable
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment

class FragmentToolLocalTalk : TrailSenseComposeFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        openApp(
            requireContext(),
            "com.kylecorry.trail_sense_comms",
            "b76f32bf-6a72-4992-a741-0c9bf19ebd11"
        )
    }

    @Composable
    override fun FragmentContent() {
        Text(
            text = "",
            style = MaterialTheme.typography.bodyMedium
        )
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
