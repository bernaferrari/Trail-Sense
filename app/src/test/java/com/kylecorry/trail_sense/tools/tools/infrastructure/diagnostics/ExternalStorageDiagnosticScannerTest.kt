package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

import android.os.Build
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.russhwolf.settings.MapSettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ExternalStorageDiagnosticScannerTest {

    @Test
    fun reportsThePackageThatDeniedAccess() {
        val prefs = deniedBy("com.android.externalstorage")
        assertEquals(
            "com.android.externalstorage",
            ExternalStorageDiagnosticScanner.deniedBy(prefs, Build.VERSION_CODES.R)
        )
    }

    @Test
    fun nothingToReportWithoutADenial() {
        val prefs = Preferences(MapSettings())
        assertNull(ExternalStorageDiagnosticScanner.deniedBy(prefs, Build.VERSION_CODES.R))

        prefs.putBoolean(IntentUriPicker.KEY_HAS_EXTERNAL_STORAGE_DENIAL, false)
        prefs.putString(IntentUriPicker.KEY_EXTERNAL_STORAGE_DENIED_BY, "com.example")
        assertNull(ExternalStorageDiagnosticScanner.deniedBy(prefs, Build.VERSION_CODES.R))
    }

    @Test
    fun nothingToReportWithoutTheDenyingPackage() {
        val prefs = Preferences(MapSettings())
        prefs.putBoolean(IntentUriPicker.KEY_HAS_EXTERNAL_STORAGE_DENIAL, true)
        assertNull(ExternalStorageDiagnosticScanner.deniedBy(prefs, Build.VERSION_CODES.R))
    }

    @Test
    fun nothingToReportBeforeAndroid11() {
        val prefs = deniedBy("com.android.externalstorage")
        assertNull(ExternalStorageDiagnosticScanner.deniedBy(prefs, Build.VERSION_CODES.Q))
    }

    @Test
    fun clearingForgetsTheDenial() {
        val prefs = deniedBy("com.android.externalstorage")
        ExternalStorageDiagnosticScanner.clearDenial(prefs)
        assertNull(ExternalStorageDiagnosticScanner.deniedBy(prefs, Build.VERSION_CODES.R))
        assertNull(prefs.getString(IntentUriPicker.KEY_EXTERNAL_STORAGE_DENIED_BY))
    }

    private fun deniedBy(packageName: String): Preferences {
        return Preferences(MapSettings()).apply {
            putBoolean(IntentUriPicker.KEY_HAS_EXTERNAL_STORAGE_DENIAL, true)
            putString(IntentUriPicker.KEY_EXTERNAL_STORAGE_DENIED_BY, packageName)
        }
    }
}
