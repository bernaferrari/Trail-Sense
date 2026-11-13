package com.kylecorry.trail_sense.settings.ui

import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.appString

import com.kylecorry.trail_sense.shared.text.withWebLinks


import androidx.compose.runtime.Composable
import com.kylecorry.trail_sense.settings.licenses.Library
import com.kylecorry.trail_sense.settings.licenses.Licenses
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection

@Composable
fun LicensesToolScreen() {
    val host = AppHost.current()
    SettingsList(title = appString(Res.string.pref_open_source_licenses_title)) {
        trailSenseSection("libraries") {
            Licenses.libraries.forEach { library ->
                row { position ->
                    SettingsActionRow(
                        label = library.name,
                        summary = library.url,
                        onClick = { showLicense(host, library) },
                        position = position
                    )
                }
            }
        }
    }
}

private fun showLicense(host: AppHost, library: Library) {
    val content = (library.url + "\n\n" + library.license()).withWebLinks()

    Alerts.dialog(
        host,
        library.name,
        content,
        cancelText = null,
        allowLinks = true
    )
}
