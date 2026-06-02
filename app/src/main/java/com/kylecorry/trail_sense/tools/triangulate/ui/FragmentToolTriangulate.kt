package com.kylecorry.trail_sense.tools.triangulate.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.preferences.putOrRemoveCoordinate
import com.kylecorry.andromeda.preferences.putOrRemoveFloat
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.science.geophysics.Geophysics
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.AppUtils
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryMarkerColor
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.extensions.from
import com.kylecorry.trail_sense.shared.extensions.lineString
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.ConfigurableGeoJsonLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayers
import com.kylecorry.trail_sense.shared.navigation.NavControllerAppNavigation
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sharing.Share
import com.kylecorry.trail_sense.shared.views.BearingInputView
import com.kylecorry.trail_sense.shared.views.CoordinateInputView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.map.map_layers.ScaleBarLayer
import com.kylecorry.trail_sense.tools.map.ui.MapView
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle

class FragmentToolTriangulate : Fragment() {

    private var triangulateViews: TriangulateViews? = null
    private val binding: TriangulateViews
        get() = triangulateViews ?: error("Triangulate views are not ready")
    private val isBound: Boolean
        get() = triangulateViews != null

    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val appNavigation by lazy { NavControllerAppNavigation(findNavController()) }
    private val navigator by lazy { Navigator.getInstance(requireContext()) }

    private var location: Coordinate? = null

    private var shouldCalculateMyLocation = false

    private val layer = ConfigurableGeoJsonLayer()
    private val scaleBarLayer = ScaleBarLayer()

    private val radius = Distance.meters(100f)

    private val recommendedMinDistance by lazy {
        if (prefs.distanceUnits == UserPreferences.DistanceUnits.Feet) {
            Distance.feet(100f)
        } else {
            Distance.meters(30f)
        }
    }

    private val updateTimer = CoroutineTimer {
        update()
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val views = createTriangulateViews(requireContext())
        triangulateViews = views
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    TriangulateHost(
                        views = views,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.bearing1.setOnBearingChangeListener { _, _ ->
            update()
        }

        binding.bearing2.setOnBearingChangeListener { _, _ ->
            update()
        }

        binding.location1.setOnCoordinateChangeListener {
            update()
        }

        binding.location2.setOnCoordinateChangeListener {
            update()
        }

        binding.createBeacon.setOnClickListener {
            location?.let {
                AppUtils.placeBeacon(requireContext(), GeoUri(it))
            }
        }

        binding.navigate.setOnClickListener {
            location?.let {
                navigator.navigateTo(it, getString(R.string.location), BeaconOwner.Triangulate)
                appNavigation.navigate(R.id.action_navigation)
            }
        }

        binding.shareLocation.setOnClickListener {
            location?.let {
                Share.shareLocation(this, it)
            }
        }

        binding.updateGpsOverride.setOnClickListener {
            location?.let { coord ->
                prefs.locationOverride = coord
                Alerts.toast(requireContext(), getString(R.string.location_override_updated))
            }
        }

        binding.locationButtonGroup.check(if (shouldCalculateMyLocation) binding.locationButtonSelf.id else binding.locationButtonOther.id)
        binding.locationButtonGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) {
                return@addOnButtonCheckedListener
            }
            shouldCalculateMyLocation = checkedId == binding.locationButtonSelf.id
            update()
        }

        // Handle expansion
        binding.location1Expansion.setOnExpandStateChangedListener {
            binding.location1DropdownIcon.rotation = if (it) 180f else 0f
        }

        binding.location2Expansion.setOnExpandStateChangedListener {
            binding.location2DropdownIcon.rotation = if (it) 180f else 0f
        }

        // Expand the first location by default (this will change once it loads the last recorded values)
        binding.location1Expansion.expand()

        // TODO: Display the distance to the location in the title
        layer.renderer.configureLineStringRenderer(shouldRenderWithDrawLines = true)
        layer.renderer.configurePointRenderer(shouldRenderLabels = true)
        binding.map.setLayers(listOf(layer, scaleBarLayer))

        binding.resetBtn.setOnClickListener {
            reset()
        }

        update()
    }

    private fun updateMap() {
        val location1 = binding.location1.coordinate
        val location2 = binding.location2.coordinate
        val destination = location

        // Update map bounds
        val fences = listOfNotNull(
            location1,
            location2,
            destination
        ).map {
            Geofence(it, radius)
        }

        val bounds = CoordinateBounds.from(fences)

        binding.map.fitIntoView(bounds)

        // Show the locations on the map
        val points = listOfNotNull(
            location1?.let {
                GeoJsonFeature.point(
                    it,
                    id = 1,
                    name = "1",
                    color = Resources.getPrimaryMarkerColor(requireContext()),
                    strokeColor = Color.WHITE
                )
            },
            location2?.let {
                GeoJsonFeature.point(
                    it,
                    id = 2,
                    name = "2",
                    color = Resources.getPrimaryMarkerColor(requireContext()),
                    strokeColor = Color.WHITE
                )
            },
            destination?.let {
                GeoJsonFeature.point(
                    it,
                    id = 3,
                    color = AppColor.Green.color,
                    strokeColor = Color.WHITE
                )
            }
        )

        // Draw bearing lines
        val path1 = getPath(1)
        val path2 = getPath(2)
        val paths = listOfNotNull(path1, path2)

        layer.setData(GeoJsonFeatureCollection(points + paths))
    }

    private fun updateDistances() {
        if (!isBound) {
            return
        }

        binding.location1Title.text = getLocationTitle(1)
        binding.location2Title.text = getLocationTitle(2)

        // TODO: Display distance between locations
    }

    private fun getLocationTitle(locationIdx: Int): CharSequence {
        val distance = getDistanceToDestination(locationIdx)
        val formattedDistance = distance?.let {
            formatService.formatDistance(it, Units.getDecimalPlaces(it.units))
        }
        return buildSpannedString {
            append(getString(R.string.location_number, locationIdx))
            if (distance != null) {
                appendLine()
                scale(0.75f) {
                    append(
                        if (shouldCalculateMyLocation) {
                            getString(R.string.distance_away_from_self, formattedDistance)
                        } else {
                            getString(R.string.distance_away_from_destination, formattedDistance)
                        }
                    )
                }
            }
        }
    }

    private fun updateInstructions() {
        if (!isBound) {
            return
        }

        if (shouldCalculateMyLocation) {
            binding.location1Instructions.text =
                getString(R.string.triangulate_self_location_1_instructions)
            binding.location2Instructions.text =
                getString(R.string.triangulate_self_location_2_instructions)
        } else {
            binding.location1Instructions.text =
                getString(R.string.triangulate_destination_location_1_instructions)
            binding.location2Instructions.text =
                getString(
                    R.string.triangulate_destination_location_2_instructions,
                    formatService.formatDistance(
                        recommendedMinDistance,
                        Units.getDecimalPlaces(recommendedMinDistance.units)
                    )
                )
        }
    }

    private fun updateCompletionState() {
        if (!isBound) {
            return
        }

        binding.location1Title.setCompoundDrawables(
            left = if (isComplete(1)) R.drawable.ic_check_outline else R.drawable.ic_info
        )
        binding.location2Title.setCompoundDrawables(
            left = if (isComplete(2)) R.drawable.ic_check_outline else R.drawable.ic_info
        )
        CustomUiUtils.setImageColor(
            binding.location1Title,
            if (isComplete(1)) AppColor.Green.color else Resources.androidTextColorSecondary(
                requireContext()
            )
        )
        CustomUiUtils.setImageColor(
            binding.location2Title,
            if (isComplete(2)) AppColor.Green.color else Resources.androidTextColorSecondary(
                requireContext()
            )
        )
    }

    private fun getDistanceToDestination(locationIdx: Int): Distance? {
        val start =
            (if (locationIdx == 1) binding.location1.coordinate else binding.location2.coordinate)
                ?: return null
        val destination = location ?: return null
        return Distance.meters(start.distanceTo(destination)).convertTo(prefs.baseDistanceUnits)
            .toRelativeDistance()
    }

    private fun getPath(locationIdx: Int): GeoJsonFeature? {
        val destination = location
        val start =
            if (locationIdx == 1) binding.location1.coordinate else binding.location2.coordinate
        val direction = if (locationIdx == 1) binding.bearing1.bearing else binding.bearing2.bearing
        val trueNorth =
            if (locationIdx == 1) binding.bearing1.trueNorth else binding.bearing2.trueNorth

        val end = if (start != null && direction != null) {
            val declination = if (trueNorth) 0f else Geophysics.getGeomagneticDeclination(start)
            val bearing = Bearing.from(direction).withDeclination(declination)
            destination ?: start.plus(
                Distance.kilometers(1f),
                if (shouldCalculateMyLocation) bearing.inverse() else bearing
            )
        } else {
            null
        }

        return if (start != null && end != null) {
            val pts = listOf(start, end)
            GeoJsonFeature.lineString(
                if (shouldCalculateMyLocation) pts.reversed() else pts,
                locationIdx.toLong(),
                color = Resources.getPrimaryMarkerColor(requireContext()),
                lineStyle = LineStyle.Arrow
            )
        } else {
            null
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bearing1.start()
        binding.bearing2.start()
        restoreState()
        // The map width/height needs time to populate for some reason
        updateTimer.once(500)
    }

    override fun onPause() {
        super.onPause()
        updateTimer.stop()
        binding.bearing1.stop()
        binding.bearing2.stop()
        binding.location1.pause()
        binding.location2.pause()
        saveState()
    }

    override fun onDestroyView() {
        triangulateViews = null
        super.onDestroyView()
    }

    private fun update() {
        if (!isBound) {
            return
        }

        updateInstructions()
        updateCompletionState()

        val location1 = binding.location1.coordinate
        val location2 = binding.location2.coordinate
        val direction1 = binding.bearing1.bearing
        val direction2 = binding.bearing2.bearing

        if (location1 == null || location2 == null || direction1 == null || direction2 == null) {
            setLocation(null)
            return
        }

        // All information is available to triangulate
        val declination1 =
            if (binding.bearing1.trueNorth) 0f else Geophysics.getGeomagneticDeclination(location1)
        val declination2 =
            if (binding.bearing2.trueNorth) 0f else Geophysics.getGeomagneticDeclination(location2)
        val bearing1 = Bearing.from(direction1).withDeclination(declination1)
        val bearing2 = Bearing.from(direction2).withDeclination(declination2)

        val location = if (shouldCalculateMyLocation) {
            Geography.triangulateSelf(location1, bearing1, location2, bearing2)
        } else {
            Geography.triangulateDestination(location1, bearing1, location2, bearing2)
        }
        setLocation(location)
    }

    private fun setLocation(location: Coordinate?) {
        this.location = location
        if (location == null || location.latitude.isNaN() || location.longitude.isNaN()) {
            binding.triangulateTitle.title.text = getString(R.string.could_not_triangulate)
            binding.actions.isVisible = false
        } else {
            binding.triangulateTitle.title.text = formatService.formatLocation(location)
            binding.actions.isVisible = true
        }

        // Update action button visibility
        binding.navigate.isVisible = !shouldCalculateMyLocation
        binding.updateGpsOverride.isVisible = !prefs.useAutoLocation && shouldCalculateMyLocation

        updateMap()
        updateDistances()
    }

    private fun saveState() {
        val preferences = PreferencesSubsystem.getInstance(requireContext()).preferences
        preferences.putBoolean("state_triangulate_self", shouldCalculateMyLocation)
        preferences.putOrRemoveFloat("state_triangulate_bearing1", binding.bearing1.bearing)
        preferences.putOrRemoveFloat("state_triangulate_bearing2", binding.bearing2.bearing)
        preferences.putOrRemoveCoordinate(
            "state_triangulate_location1",
            binding.location1.coordinate
        )
        preferences.putOrRemoveCoordinate(
            "state_triangulate_location2",
            binding.location2.coordinate
        )
        preferences.putBoolean("state_triangulate_true_north1", binding.bearing1.trueNorth)
        preferences.putBoolean("state_triangulate_true_north2", binding.bearing2.trueNorth)
    }

    private fun restoreState() {
        val preferences = PreferencesSubsystem.getInstance(requireContext()).preferences
        shouldCalculateMyLocation = preferences.getBoolean("state_triangulate_self") ?: false
        binding.locationButtonGroup.check(if (shouldCalculateMyLocation) binding.locationButtonSelf.id else binding.locationButtonOther.id)
        binding.bearing1.bearing = preferences.getFloat("state_triangulate_bearing1")
        binding.bearing2.bearing = preferences.getFloat("state_triangulate_bearing2")
        binding.bearing1.trueNorth =
            preferences.getBoolean("state_triangulate_true_north1") ?: false
        binding.bearing2.trueNorth =
            preferences.getBoolean("state_triangulate_true_north2") ?: false
        binding.location1.coordinate = preferences.getCoordinate("state_triangulate_location1")
        binding.location2.coordinate = preferences.getCoordinate("state_triangulate_location2")
        update()
    }

    private fun reset() {
        binding.location1.coordinate = null
        binding.location2.coordinate = null
        binding.bearing1.bearing = null
        binding.bearing2.bearing = null
        binding.bearing1.trueNorth = false
        binding.bearing2.trueNorth = false

        val preferences = PreferencesSubsystem.getInstance(requireContext()).preferences
        preferences.remove("state_triangulate_self")
        preferences.remove("state_triangulate_bearing1")
        preferences.remove("state_triangulate_bearing2")
        preferences.remove("state_triangulate_location1")
        preferences.remove("state_triangulate_location2")
        preferences.remove("state_triangulate_true_north1")
        preferences.remove("state_triangulate_true_north2")

        update()
    }

    private fun isComplete(locationIdx: Int): Boolean {
        val location =
            if (locationIdx == 1) binding.location1.coordinate else binding.location2.coordinate
        val bearing = if (locationIdx == 1) binding.bearing1.bearing else binding.bearing2.bearing
        return location != null && bearing != null
    }

}

private data class TriangulateViews(
    val root: View,
    val triangulateTitle: TriangulateTitle,
    val actions: ChipGroup,
    val createBeacon: Chip,
    val navigate: Chip,
    val shareLocation: Chip,
    val updateGpsOverride: Chip,
    val map: MapView,
    val locationButtonGroup: MaterialButtonToggleGroup,
    val locationButtonOther: MaterialButton,
    val locationButtonSelf: MaterialButton,
    val location1Expansion: TriangulateExpansion,
    val location1Title: TextView,
    val location1DropdownIcon: ImageView,
    val location1Instructions: TextView,
    val location1: CoordinateInputView,
    val bearing1: BearingInputView,
    val location2Expansion: TriangulateExpansion,
    val location2Title: TextView,
    val location2DropdownIcon: ImageView,
    val location2Instructions: TextView,
    val location2: CoordinateInputView,
    val bearing2: BearingInputView,
    val resetBtn: MaterialButton
)

private data class TriangulateTitle(
    val title: TextView
)

private class TriangulateExpansion(
    val root: View,
    private val content: View,
    header: View
) {
    private var listener: ((Boolean) -> Unit)? = null
    private var expanded = false

    init {
        content.isVisible = false
        header.setOnClickListener {
            setExpanded(!expanded)
        }
    }

    fun setOnExpandStateChangedListener(listener: ((Boolean) -> Unit)?) {
        this.listener = listener
    }

    fun expand() {
        setExpanded(true)
    }

    private fun setExpanded(expanded: Boolean) {
        if (this.expanded == expanded) {
            return
        }
        this.expanded = expanded
        content.isVisible = expanded
        listener?.invoke(expanded)
    }
}

@Composable
private fun TriangulateHost(
    views: TriangulateViews,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

private data class TriangulateLocationSection(
    val expansion: TriangulateExpansion,
    val title: TextView,
    val dropdownIcon: ImageView,
    val instructions: TextView,
    val coordinate: CoordinateInputView,
    val bearing: BearingInputView
)

private fun createTriangulateViews(context: Context): TriangulateViews {
    val root = ScrollView(context).apply {
        isFillViewport = false
    }
    val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, 0, 0, context.resources.getDimensionPixelSize(R.dimen.default_bottom_margin))
    }
    root.addView(
        content,
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    val titleText = TextView(context).apply {
        id = R.id.triangulate_title
        textSize = 20f
        typeface = Typeface.DEFAULT_BOLD
        setPadding(16.dp(context), 16.dp(context), 16.dp(context), 8.dp(context))
        setTextColor(resolveThemeColor(context, android.R.attr.textColorPrimary))
    }
    content.addView(
        titleText,
        linearParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    )

    val actions = ChipGroup(context).apply {
        id = R.id.actions
        visibility = View.GONE
    }
    val createBeacon = createActionChip(
        context,
        R.id.create_beacon,
        context.getString(R.string.beacon),
        R.drawable.ic_location
    )
    val navigate = createActionChip(
        context,
        R.id.navigate,
        context.getString(R.string.navigate),
        R.drawable.ic_beacon
    )
    val shareLocation = createActionChip(
        context,
        R.id.share_location,
        context.getString(R.string.share_ellipsis),
        R.drawable.ic_share
    )
    val updateGpsOverride = createActionChip(
        context,
        R.id.update_gps_override,
        context.getString(R.string.update_gps_override),
        R.drawable.satellite
    )
    actions.addView(createBeacon)
    actions.addView(navigate)
    actions.addView(shareLocation)
    actions.addView(updateGpsOverride)
    content.addView(
        actions,
        linearParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            start = 16.dp(context),
            end = 16.dp(context)
        )
    )

    val map = MapView(context).apply {
        id = R.id.map
        background = ContextCompat.getDrawable(context, R.drawable.rounded_rectangle)
        backgroundTintList = ColorStateList.valueOf(Resources.color(context, R.color.colorSecondary))
        backgroundColorOverride = Resources.color(context, R.color.colorSecondary)
    }
    content.addView(
        map,
        linearParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            200.dp(context),
            start = 8.dp(context),
            top = 8.dp(context),
            end = 8.dp(context)
        )
    )

    content.addView(
        TextView(context).apply {
            text = context.getString(R.string.location_to_triangulate)
        },
        linearParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            start = 8.dp(context),
            top = 16.dp(context),
            end = 8.dp(context)
        )
    )

    val locationButtonGroup = MaterialButtonToggleGroup(context).apply {
        id = R.id.location_button_group
        isSingleSelection = true
    }
    val locationButtonOther = MaterialButton(context).apply {
        id = R.id.location_button_other
        text = context.getString(R.string.destination)
    }
    val locationButtonSelf = MaterialButton(context).apply {
        id = R.id.location_button_self
        text = context.getString(R.string.my_location)
    }
    locationButtonGroup.addView(
        locationButtonOther,
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    )
    locationButtonGroup.addView(
        locationButtonSelf,
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    )
    content.addView(
        locationButtonGroup,
        linearParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            start = 8.dp(context),
            top = 8.dp(context),
            end = 8.dp(context)
        )
    )

    val location1 = createLocationSection(context, 1)
    content.addView(
        location1.expansion.root,
        linearParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            start = 8.dp(context),
            top = 16.dp(context),
            end = 8.dp(context)
        )
    )

    val location2 = createLocationSection(context, 2)
    content.addView(
        location2.expansion.root,
        linearParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            start = 8.dp(context),
            top = 16.dp(context),
            end = 8.dp(context)
        )
    )

    val resetBtn = MaterialButton(context).apply {
        id = R.id.reset_btn
        text = context.getString(R.string.reset)
    }
    content.addView(
        resetBtn,
        linearParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            top = 16.dp(context),
            gravity = Gravity.CENTER_HORIZONTAL
        )
    )

    return TriangulateViews(
        root = root,
        triangulateTitle = TriangulateTitle(titleText),
        actions = actions,
        createBeacon = createBeacon,
        navigate = navigate,
        shareLocation = shareLocation,
        updateGpsOverride = updateGpsOverride,
        map = map,
        locationButtonGroup = locationButtonGroup,
        locationButtonOther = locationButtonOther,
        locationButtonSelf = locationButtonSelf,
        location1Expansion = location1.expansion,
        location1Title = location1.title,
        location1DropdownIcon = location1.dropdownIcon,
        location1Instructions = location1.instructions,
        location1 = location1.coordinate,
        bearing1 = location1.bearing,
        location2Expansion = location2.expansion,
        location2Title = location2.title,
        location2DropdownIcon = location2.dropdownIcon,
        location2Instructions = location2.instructions,
        location2 = location2.coordinate,
        bearing2 = location2.bearing,
        resetBtn = resetBtn
    )
}

private fun createActionChip(
    context: Context,
    id: Int,
    text: String,
    icon: Int
): Chip {
    return Chip(context).apply {
        this.id = id
        this.text = text
        setChipIconResource(icon)
        chipIconSize = context.resources.getDimension(R.dimen.chip_icon_size)
        chipIconTint = ColorStateList.valueOf(Resources.androidTextColorSecondary(context))
        iconStartPadding = 4.dp(context).toFloat()
    }
}

private fun createLocationSection(
    context: Context,
    index: Int
): TriangulateLocationSection {
    val root = LinearLayout(context).apply {
        id = if (index == 1) R.id.location_1_expansion else R.id.location_2_expansion
        orientation = LinearLayout.VERTICAL
        background = ContextCompat.getDrawable(context, R.drawable.rounded_rectangle)
        backgroundTintList = ColorStateList.valueOf(
            resolveThemeColor(context, android.R.attr.colorBackgroundFloating)
        )
    }

    val header = LinearLayout(context).apply {
        gravity = Gravity.CENTER_VERTICAL
        orientation = LinearLayout.HORIZONTAL
        setPadding(16.dp(context), 16.dp(context), 16.dp(context), 16.dp(context))
    }
    val title = TextView(context).apply {
        id = if (index == 1) R.id.location_1_title else R.id.location_2_title
        gravity = Gravity.CENTER_VERTICAL
        compoundDrawablePadding = 8.dp(context)
        setCompoundDrawables(left = R.drawable.ic_info)
        CustomUiUtils.setImageColor(this, Resources.androidTextColorSecondary(context))
    }
    val dropdown = ImageView(context).apply {
        id = if (index == 1) R.id.location_1_dropdown_icon else R.id.location_2_dropdown_icon
        setImageResource(R.drawable.ic_drop_down)
        imageTintList = ColorStateList.valueOf(Resources.androidTextColorSecondary(context))
    }
    header.addView(
        title,
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    )
    header.addView(
        dropdown,
        LinearLayout.LayoutParams(24.dp(context), 24.dp(context))
    )
    root.addView(
        header,
        linearParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    )

    val body = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(16.dp(context), 0, 16.dp(context), 16.dp(context))
    }
    val instructions = TextView(context).apply {
        id = if (index == 1) R.id.location_1_instructions else R.id.location_2_instructions
    }
    val coordinate = CoordinateInputView(context).apply {
        id = if (index == 1) R.id.location_1 else R.id.location_2
    }
    val bearing = BearingInputView(context).apply {
        id = if (index == 1) R.id.bearing_1 else R.id.bearing_2
    }
    body.addView(
        instructions,
        linearParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    )
    body.addView(
        coordinate,
        linearParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            top = 24.dp(context)
        )
    )
    body.addView(
        bearing,
        linearParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            top = 24.dp(context)
        )
    )
    root.addView(
        body,
        linearParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    )

    return TriangulateLocationSection(
        expansion = TriangulateExpansion(root, body, header),
        title = title,
        dropdownIcon = dropdown,
        instructions = instructions,
        coordinate = coordinate,
        bearing = bearing
    )
}

private fun linearParams(
    width: Int,
    height: Int,
    start: Int = 0,
    top: Int = 0,
    end: Int = 0,
    bottom: Int = 0,
    gravity: Int? = null
): LinearLayout.LayoutParams {
    return LinearLayout.LayoutParams(width, height).apply {
        setMargins(start, top, end, bottom)
        gravity?.let { this.gravity = it }
    }
}

private fun resolveThemeColor(context: Context, attr: Int): Int {
    val value = TypedValue()
    context.theme.resolveAttribute(attr, value, true)
    return if (value.resourceId != 0) {
        ContextCompat.getColor(context, value.resourceId)
    } else {
        value.data
    }
}

private fun Int.dp(context: Context): Int {
    return Resources.dp(context, toFloat()).toInt()
}

@Preview(showBackground = true)
@Composable
private fun TriangulateHostPreview() {
    TrailSenseTheme {
        TriangulateHost(
            views = createTriangulateViews(androidx.compose.ui.platform.LocalContext.current)
        )
    }
}
