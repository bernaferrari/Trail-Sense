package com.kylecorry.trail_sense.tools.field_guide.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard

@Composable
fun Fragment.CreateFieldGuideSightingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TrailSenseCard {
            Column(Modifier.padding(16.dp)) {
                Text("Create Sighting", style = MaterialTheme.typography.titleLarge)
                Button(onClick = { }) {
                    Text("Save")
                }
            }
        }
    }
}

class CreateFieldGuideSightingFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        CreateFieldGuideSightingContent()
    }
}
        }

        val hasChanges = useMemo(
            initialSighting,
            initialDatetime,
            datetime,
            location,
            elevation,
            notes,
            harvested,
            showOnMap
        ) {
            if (initialSighting != null) {
                val initialHarvested = initialSighting.harvested ?: false
                datetime?.toZonedDateTime()?.toInstant() != initialSighting.time ||
                        location != initialSighting.location ||
                        elevation?.meters()?.value != initialSighting.altitude ||
                        notes != initialSighting.notes ||
                        harvested != initialHarvested ||
                        showOnMap != initialSighting.showOnMap
            } else {
                datetime != initialDatetime ||
                        location != null ||
                        elevation != null ||
                        !notes.isNullOrEmpty() ||
                        harvested ||
                        !showOnMap
            }
        }

        val page = useBackgroundMemo(repo, pageId) {
            repo.getPage(pageId)
        }

        // Effects
        useEffect(datetimeView) {
            datetimeView.setOnItemSelectedListener {
                setDatetime(it)
            }
            val initial = LocalDateTime.now()
            setInitialDatetime(initial)
            datetimeView.setValue(initial)
        }

        useEffect(coordinateView) {
            coordinateView.setOnCoordinateChangeListener {
                setLocation(it)
            }
        }

        useEffect(elevationView) {
            elevationView.setOnElevationChangeListener {
                setElevation(it)
            }
        }

        useEffect(notesView) {
            notesView.addTextChangedListener {
                setNotes(it?.toString())
            }
        }

        useEffect(harvestedSwitchView) {
            harvestedSwitchView.setOnCheckedChangeListener { _, isChecked ->
                setHarvested(isChecked)
            }
        }

        useEffect(showOnMapSwitchView) {
            showOnMapSwitchView.setOnCheckedChangeListener { _, isChecked ->
                setShowOnMap(isChecked)
            }
        }

        useBackgroundEffect(
            initialSighting,
            datetimeView,
            coordinateView,
            elevationView,
            notesView,
            harvestedSwitchView,
            showOnMapSwitchView,
            repo,
            prefs
        ) {
            val sighting = initialSighting ?: return@useBackgroundEffect
            val elevation = sighting.altitude?.let {
                Distance.meters(it).convertTo(prefs.baseDistanceUnits)
            }
            // Update views
            sighting.time?.let {
                datetimeView.setValue(it.toZonedDateTime().toLocalDateTime())
            }
            coordinateView.coordinate = sighting.location
            elevationView.elevation = elevation
            notesView.setText(sighting.notes)
            harvestedSwitchView.isChecked = sighting.harvested == true
            showOnMapSwitchView.isChecked = sighting.showOnMap

            // Update state
            setDatetime(sighting.time?.toZonedDateTime()?.toLocalDateTime())
            setLocation(sighting.location)
            setElevation(elevation)
            setNotes(sighting.notes)
            setHarvested(sighting.harvested == true)
            setShowOnMap(sighting.showOnMap)
        }

        useEffect(
            titleView,
            context,
            navController,
            repo,
            sightingId,
            pageId,
            datetime,
            location,
            elevation,
            harvested,
            notes,
            showOnMap
        ) {
            CustomUiUtils.setButtonState(titleView.rightButton, true)
            titleView.rightButton.setOnClickListener {
                inBackground {
                    Alerts.withLoading(context, getString(R.string.saving)) {
                        repo.addSighting(
                            Sighting(
                                sightingId,
                                pageId,
                                datetime?.toZonedDateTime()?.toInstant(),
                                location,
                                elevation?.meters()?.value,
                                harvested,
                                notes,
                                showOnMap
                            )
                        )
                        onMain {
                            navController.navigateUp()
                        }
                    }
                }
            }
        }

        useEffect(titleView, page) {
            titleView.subtitle.text = page?.let(fieldGuideFormatter::formatName)
        }

        useBindCoordinateAndElevationViews(coordinateView, elevationView)

        useUnsavedChangesPrompt(hasChanges)
    }
}
