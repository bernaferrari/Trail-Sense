package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Warns when a file the user picked couldn't be read because another app (a file provider) denied access: the
 * picker records the denial, and the action opens that app's "all files access" setting.
 */
class ExternalStorageDiagnosticScanner(
    private val preferences: () -> Preferences = { getAppService<PreferencesSubsystem>().preferences },
    private val sdkInt: Int = Build.VERSION.SDK_INT
) : ToolDiagnosticScanner {

    override fun quickScan(context: Context): List<ToolDiagnosticResult> {
        val prefs = preferences()
        val packageName = deniedBy(prefs, sdkInt) ?: return emptyList()
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = "package:$packageName".toUri()
        }
        return listOf(
            ToolDiagnosticResult(
                ID,
                ToolDiagnosticSeverity.Warning,
                context.getString(R.string.external_storage),
                context.getString(R.string.no_permission),
                context.getString(R.string.external_storage_diagnostic_instructions),
                ToolDiagnosticAction(context.getString(R.string.settings)) { actionContext, _ ->
                    clearDenial(prefs)
                    actionContext.startActivity(intent)
                }
            )
        )
    }

    override fun fullScan(context: Context): Flow<List<ToolDiagnosticResult>> {
        return flowOf(quickScan(context))
    }

    companion object {
        const val ID = "external-storage-no-permission"

        /** The package that denied the last picked file, when there's a setting (Android 11+) that can fix it. */
        fun deniedBy(prefs: Preferences, sdkInt: Int): String? {
            if (sdkInt < Build.VERSION_CODES.R) {
                return null
            }
            if (prefs.getBoolean(IntentUriPicker.KEY_HAS_EXTERNAL_STORAGE_DENIAL) != true) {
                return null
            }
            return prefs.getString(IntentUriPicker.KEY_EXTERNAL_STORAGE_DENIED_BY)
        }

        fun clearDenial(prefs: Preferences) {
            prefs.remove(IntentUriPicker.KEY_HAS_EXTERNAL_STORAGE_DENIAL)
            prefs.remove(IntentUriPicker.KEY_EXTERNAL_STORAGE_DENIED_BY)
        }
    }
}
