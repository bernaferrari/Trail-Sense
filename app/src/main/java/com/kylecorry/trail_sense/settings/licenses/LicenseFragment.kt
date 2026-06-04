package com.kylecorry.trail_sense.settings.licenses

import android.text.util.Linkify.WEB_URLS
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.text.toSpannable
import androidx.core.text.util.LinkifyCompat
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing


class LicenseFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = TrailSenseSpacing.lg,
                    end = TrailSenseSpacing.lg,
                    top = TrailSenseSpacing.lg,
                    bottom = TrailSenseSpacing.xxxl
                )
            ) {
                item("header") {
                    TrailSenseSectionHeader(
                        title = stringResource(R.string.pref_open_source_licenses_title)
                    )
                }
                items(Licenses.libraries, key = { it.name }) { library ->
                    SettingsActionRow(
                        label = library.name,
                        summary = library.url,
                        onClick = { showLicense(library) },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                }
            }
        }
    }

    private fun showLicense(library: Library) {
        val content = (library.url + "\n\n" + library.license()).toSpannable()
        LinkifyCompat.addLinks(content, WEB_URLS)

        Alerts.dialog(
            requireContext(),
            library.name,
            content,
            cancelText = null,
            allowLinks = true
        )
    }

}
