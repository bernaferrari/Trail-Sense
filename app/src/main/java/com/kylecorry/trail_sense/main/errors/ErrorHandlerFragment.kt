package com.kylecorry.trail_sense.main.errors

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.andromeda.clipboard.Clipboard
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.extensions.compose.useArgument
import com.kylecorry.trail_sense.shared.openTool

/**
 * Error handler screen — now on Compose (Ralph Loop).
 */
class ErrorHandlerFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        val context = LocalContext.current
        val toolId = arguments?.getLong("tool_id") ?: 0L
        val errorDetails = arguments?.getString("error") ?: ""
        ErrorHandlerContent(
            toolId = toolId,
            errorDetails = errorDetails,
            onEmail = { details ->
                val intent = Intents.email(
                    getString(R.string.email),
                    "Error in ${getString(R.string.app_name)}",
                    details
                )
                startActivity(intent)
            },
            onCopy = { details ->
                Clipboard.copy(context, details, getString(R.string.copied_to_clipboard_toast))
            },
            onReopenTool = { id -> requireActivity().openTool(id) }
        )
    }
}
