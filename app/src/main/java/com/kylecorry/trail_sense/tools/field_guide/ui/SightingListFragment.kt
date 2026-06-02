package com.kylecorry.trail_sense.tools.field_guide.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemTag
import com.kylecorry.andromeda.views.list.ListMenuItem
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.Sighting
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SightingListFragment : Fragment() {

    private val formatter by lazy { FormatService.getInstance(requireContext()) }
    private val repo by lazy { FieldGuideRepo.getInstance(requireContext()) }
    private val navigator by lazy { Navigator.getInstance(requireContext()) }
    private val pageId by lazy { requireArguments().getLong("page_id") }

    private var page by mutableStateOf<FieldGuidePage?>(null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    SightingListScreen(
                        items = getSightingListItems(),
                        onCreate = ::createSighting
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadPage()
    }

    private fun loadPage() {
        viewLifecycleOwner.lifecycleScope.launch {
            page = withContext(Dispatchers.IO) {
                repo.getPage(pageId)
            }
        }
    }

    private fun getSightingListItems(): List<ListItem> {
        val currentPage = page
        val sightings = currentPage?.sightings?.sortedByDescending { it.time } ?: emptyList()
        return sightings.map {
            ListItem(
                it.id,
                it.time?.let { time ->
                    formatter.formatRelativeDateTime(
                        time.toZonedDateTime(),
                        includeSeconds = false
                    )
                } ?: getString(R.string.sighting),
                it.notes,
                tags = listOfNotNull(
                    if (it.harvested == true) {
                        ListItemTag(
                            getString(R.string.harvested),
                            null,
                            Resources.androidTextColorSecondary(requireContext())
                        )
                    } else {
                        null
                    }
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

    private fun createSighting() {
        findNavController().navigateWithAnimation(
            R.id.createFieldGuideSightingFragment,
            Bundle().apply {
                putLong("page_id", pageId)
            }
        )
    }

    private fun editSighting(sighting: Sighting) {
        findNavController().navigateWithAnimation(
            R.id.createFieldGuideSightingFragment,
            Bundle().apply {
                putLong("page_id", pageId)
                putLong("sighting_id", sighting.id)
            }
        )
    }

    private fun deleteSighting(sighting: Sighting) {
        Alerts.dialog(
            requireContext(),
            getString(R.string.delete),
            sighting.time?.let { time ->
                formatter.formatRelativeDateTime(
                    time.toZonedDateTime(),
                    includeSeconds = false
                )
            } ?: getString(R.string.sighting)
        ) { cancelled ->
            if (!cancelled) {
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        repo.deleteSighting(sighting)
                    }
                    loadPage()
                }
            }
        }
    }

    private fun navigateToSighting(sighting: Sighting) {
        navigator.navigateTo(
            sighting.location ?: Coordinate.zero,
            page?.name ?: "",
            BeaconOwner.FieldGuide,
            elevation = sighting.altitude
        )
        findNavController().openTool(Tools.NAVIGATION)
    }

    private fun createBeacon(sighting: Sighting) {
        val bundle = Bundle().apply {
            putParcelable(
                "initial_location",
                GeoUri(
                    sighting.location ?: Coordinate.zero,
                    sighting.altitude,
                    mapOf("label" to (page?.name ?: ""))
                )
            )
        }
        findNavController().navigateWithAnimation(R.id.placeBeaconFragment, bundle)
    }
}

@Composable
private fun SightingListScreen(
    items: List<ListItem>,
    onCreate: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                SightingListHeader()
                SightingListView(
                    items = items,
                    modifier = Modifier.fillMaxSize()
                )
            }
            FloatingActionButton(
                onClick = onCreate,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("create_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun SightingListHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = stringResource(R.string.sightings),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SightingListView(
    items: List<ListItem>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.testTag("list"),
        factory = { context ->
            FrameLayout(context).apply {
                val list = AndromedaListView(context, null).apply {
                    id = R.id.list
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                val empty = TextView(context).apply {
                    text = context.getString(R.string.no_sightings)
                    gravity = Gravity.CENTER
                    visibility = View.GONE
                }
                list.emptyView = empty
                addView(list)
                addView(
                    empty,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                    )
                )
            }
        },
        update = { container ->
            container.findViewById<AndromedaListView>(R.id.list)
                .setItems(items)
        }
    )
}
