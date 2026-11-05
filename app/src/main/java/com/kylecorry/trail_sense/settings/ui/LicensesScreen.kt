package com.kylecorry.trail_sense.settings.ui

import android.content.Context

import androidx.compose.ui.platform.LocalContext

import android.text.util.Linkify.WEB_URLS
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.text.toSpannable
import androidx.core.text.util.LinkifyCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.licenses.Library
import com.kylecorry.trail_sense.settings.licenses.Licenses
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection

@Composable
fun LicensesToolScreen() {
    val context = LocalContext.current
    SettingsList(title = stringResource(R.string.pref_open_source_licenses_title)) {
        trailSenseSection("libraries") {
            Licenses.libraries.forEach { library ->
                row { position ->
                    SettingsActionRow(
                        label = library.name,
                        summary = library.url,
                        onClick = { context.showLicense(library) },
                        position = position
                    )
                }
            }
        }
    }
}

private fun Context.showLicense(library: Library) {
    val content = (library.url + "\n\n" + library.license()).toSpannable()
    LinkifyCompat.addLinks(content, WEB_URLS)

    Alerts.dialog(
        this,
        library.name,
        content,
        cancelText = null,
        allowLinks = true
    )
}
