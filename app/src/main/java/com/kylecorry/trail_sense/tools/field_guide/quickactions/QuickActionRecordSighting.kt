package com.kylecorry.trail_sense.tools.field_guide.quickactions

import com.kylecorry.trail_sense.shared.views.compose.AndroidDestinationHost
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.dialogs.showComposeDialog
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.andromeda.sense.readAll
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.extensions.withSuspendableCancelableLoading
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.field_guide.ui.FieldGuideFormatService
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.Sighting
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import kotlinx.coroutines.launch
import kotlin.time.Clock
import com.kylecorry.trail_sense.shared.views.compose.from

/**
 * Records a sighting on a field guide page picked from a searchable list, at the current location, as upstream's
 * "Record sighting" quick action did (#3860). Long press opens the field guide.
 */
class QuickActionRecordSighting(btn: QuickActionButtonView, host: AndroidDestinationHost) :
    QuickActionButton(btn, host) {

    private val repo by lazy { FieldGuideRepo.getInstance(host.context) }

    override fun onCreate() {
        super.onCreate()
        setIcon(TrailIcons.FieldGuide)
    }

    override fun onClick() {
        super.onClick()
        host.lifecycleOwner.lifecycleScope.launch {
            val pages = repo.getAllPages().sortedBy { it.name.lowercase() }
            showPagePicker(pages) { page -> recordSighting(page) }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun showPagePicker(pages: List<FieldGuidePage>, onPick: (FieldGuidePage) -> Unit) {
        val context = host.context
        showComposeDialog(context) { dismiss ->
            var query by remember { mutableStateOf("") }
            val matches = remember(query) {
                pages.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
            }
            Text(
                text = stringResource(R.string.record_sighting),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search)) },
                modifier = Modifier
                    .semantics { testTagsAsResourceId = true }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .testTag("search")
            )
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                items(matches, key = { it.id }) { page ->
                    Text(
                        text = page.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                dismiss()
                                onPick(page)
                            }
                            .padding(horizontal = 24.dp, vertical = 14.dp)
                    )
                }
            }
            TextButton(
                onClick = dismiss,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    }

    private fun recordSighting(page: FieldGuidePage) {
        val context = host.context
        val navigator = host.navigator
        host.lifecycleOwner.lifecycleScope.launch {
            // As upstream: take a fresh fix rather than a stale (or unknown, 0, 0) cached location
            var reading: Pair<Coordinate, Float>? = null
            var cancelled = false
            val job = launch {
                val sensors = getAppService<SensorService>()
                val gps = sensors.getGPS(tag = "QuickActionRecordSighting")
                val altimeter = sensors.getAltimeter(gps = gps, tag = "QuickActionRecordSighting")
                readAll(listOf(gps, altimeter))
                if (gps.hasValidReading) {
                    reading = gps.location to altimeter.altitude
                }
            }

            val pageName = FieldGuideFormatService(getAppService()).formatName(page)
            Alerts.withSuspendableCancelableLoading(
                context,
                context.getString(R.string.recording_sighting_for_page, pageName),
                onCancel = {
                    cancelled = true
                    job.cancel()
                }
            ) {
                job.join()
            }

            if (cancelled) {
                return@launch
            }

            val fix = reading
            if (fix == null) {
                // No fix: open a new sighting for the page so the user can fill in where it was
                navigator.navigate(Route.FieldGuidePage(pageId = page.id))
                navigator.navigate(Route.SightingList(pageId = page.id))
                navigator.navigate(Route.CreateFieldGuideSighting(pageId = page.id))
                return@launch
            }

            val id = repo.addSighting(
                Sighting(
                    0,
                    page.id,
                    Clock.System.now(),
                    fix.first,
                    fix.second
                )
            )
            AppHost.from(context).snackbar(
                context.getString(R.string.created),
                context.getString(R.string.view),
                long = true
            ) {
                // As upstream's deep link: the page and its sightings sit under the sighting, so saving it returns to
                // the sightings list
                navigator.navigate(Route.FieldGuidePage(pageId = page.id))
                navigator.navigate(Route.SightingList(pageId = page.id))
                navigator.navigate(Route.CreateFieldGuideSighting(pageId = page.id, sightingId = id))
            }
        }
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.navigator.navigate(Route.FieldGuide)
        return true
    }
}
