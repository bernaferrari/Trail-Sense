package com.kylecorry.trail_sense.tools.field_guide.ui

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.tools.field_guide.domain.Sighting
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import kotlinx.coroutines.launch

@Composable
fun Fragment.SightingListContent(modifier: Modifier = Modifier) {
    val context = requireContext()
    val navController = findNavController()
    val scope = rememberCoroutineScope()

    val repo = remember { FieldGuideRepo.getInstance(context) }
    val pageId = remember { arguments?.getLong("page_id") ?: 0L }

    var sightings by remember { mutableStateOf<List<Sighting>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(pageId) {
        isLoading = true
        sightings = repo.getSightings(pageId).sortedByDescending { it.timestamp }
        isLoading = false
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isLoading) {
            Text(
                "Loading sightings...",
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (sightings.isEmpty()) {
            TrailSenseEmptyState(
                message = getString(R.string.no_sightings),
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                itemsIndexed(sightings) { index, sighting ->
                    TrailSenseCard(
                        onClick = {
                            val bundle = Bundle().apply {
                                putLong("page_id", pageId)
                                putLong("sighting_id", sighting.id)
                            }
                            navController.navigate(R.id.fieldGuideSightingFragment, bundle)
                        }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = sighting.timestamp.toString(),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${sighting.coordinate.latitude}, ${sighting.coordinate.longitude}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                val bundle = Bundle().apply {
                    putLong("page_id", pageId)
                }
                navController.navigate(R.id.createFieldGuideSightingFragment, bundle)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = getString(R.string.create))
        }
    }
}

class SightingListFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        SightingListContent()
    }
}

        val sightings = useMemo(page) {
            page?.sightings?.sortedByDescending { it.time } ?: emptyList()
        }

        val deleteSighting = useCallback(context, repo) { sighting: Sighting ->
            Alerts.dialog(
                context,
                getString(R.string.delete),
                sighting.time?.let { time ->
                    formatter.formatRelativeDateTime(
                        time.toZonedDateTime(),
                        includeSeconds = false
                    )
                }
                    ?: getString(R.string.sighting)) { cancelled ->
                if (!cancelled) {
                    inBackground {
                        repo.deleteSighting(sighting)
                        triggerRefresh()
                    }
                }
            }
        }

        val editSighting = useCallback(navController) { sighting: Sighting ->
            navController.navigateWithAnimation(
                R.id.createFieldGuideSightingFragment, Bundle().apply {
                    putLong("page_id", pageId)
                    putLong("sighting_id", sighting.id)
                }
            )
        }

        val navigateToSighting =
            useCallback(navigator, navController, page, fieldGuideFormatter) { sighting: Sighting ->
                navigator.navigateTo(
                    sighting.location ?: Coordinate.zero,
                    page?.let(fieldGuideFormatter::formatName)?.toString() ?: "",
                    BeaconOwner.FieldGuide,
                    elevation = sighting.altitude
                )
                // TODO: Let user set default navigation tool (compass or map)
                navController.openTool(Tools.NAVIGATION)
            }

        val createBeacon = useCallback(navController, page, fieldGuideFormatter) { sighting: Sighting ->
            val bundle = Bundle().apply {
                putParcelable(
                    "initial_location", GeoUri(
                        sighting.location ?: Coordinate.zero,
                        sighting.altitude,
                        mapOf("label" to (page?.let(fieldGuideFormatter::formatName)?.toString() ?: ""))
                    )
                )
            }
            navController.navigateWithAnimation(R.id.placeBeaconFragment, bundle)
        }

        val sightingListItems = useMemo(
            formatter,
            sightings,
            deleteSighting,
            editSighting,
            navigateToSighting,
            createBeacon
        ) {
            sightings.map {
                ListItem(
                    it.id,
                    it.time?.let { time ->
                        formatter.formatRelativeDateTime(
                            time.toZonedDateTime(),
                            includeSeconds = false
                        )
                    }
                        ?: getString(R.string.sighting),
                    it.notes,
                    tags = listOfNotNull(
                        if (it.harvested == true) ListItemTag(
                            getString(R.string.harvested),
                            null,
                            Resources.androidTextColorSecondary(context)
                        ) else null
                    ),
                    menu = listOfNotNull(
                        if (it.location != null) ListMenuItem(getString(R.string.navigate)) {
                            navigateToSighting(it)
                        } else null,
                        if (it.location != null) ListMenuItem(getString(R.string.create_beacon)) {
                            createBeacon(it)
                        } else null,
                        ListMenuItem(getString(R.string.edit)) {
                            editSighting(it)
                        },
                        ListMenuItem(getString(R.string.delete)) {
                            deleteSighting(it)
                        }
                    )
                ) {
                    editSighting(it)
                }
            }
        }

        // Effects
        useEffect(listView, emptyTextView) {
            listView.emptyView = emptyTextView
        }

        useEffect(listView, sightingListItems) {
            listView.setItems(sightingListItems)
        }

        useEffect(createButtonView, pageId, navController) {
            createButtonView.setOnClickListener {
                navController.navigateWithAnimation(
                    R.id.createFieldGuideSightingFragment,
                    Bundle().apply {
                        putLong("page_id", pageId)
                    }
                )
            }
        }

        useEffect(titleView, page) {
            titleView.subtitle.text = page?.let(fieldGuideFormatter::formatName)
        }

    }
}
