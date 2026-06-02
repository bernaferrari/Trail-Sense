package com.kylecorry.trail_sense.tools.beacons.ui.list

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.luna.extensions.filterIndices
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.onBackPressed
import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.from
import com.kylecorry.trail_sense.shared.grouping.lists.GroupListManager
import com.kylecorry.trail_sense.shared.grouping.lists.bind
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.FloatingActionButtonMenu
import com.kylecorry.trail_sense.shared.views.SearchView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.tools.beacons.domain.IBeacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.CreateBeaconGroupCommand
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.DeleteBeaconCommand
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.MoveBeaconCommand
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.MoveBeaconGroupCommand
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.RenameBeaconGroupCommand
import com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.cell.NavigateToNearestCellSignal
import com.kylecorry.trail_sense.tools.beacons.infrastructure.export.BeaconGpxConverter
import com.kylecorry.trail_sense.tools.beacons.infrastructure.export.BeaconGpxImporter
import com.kylecorry.trail_sense.tools.beacons.infrastructure.loading.BeaconLoader
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.share.BeaconSender
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.BeaconSortMethod
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.ClosestBeaconSort
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.MostRecentBeaconSort
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.NameBeaconSort
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.qr.infrastructure.BeaconQREncoder
import java.time.Instant

class BeaconListFragment : AndromedaFragment() {

    private var beaconViews: BeaconListViews? = null
    private val binding: BeaconListViews
        get() = beaconViews ?: error("Beacon list views are not ready")

    private val gps by lazy { sensorService.getGPS() }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private lateinit var navController: NavController
    private val sensorService by lazy { SensorService(requireContext()) }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val beaconService by lazy { BeaconService(requireContext()) }
    private val beaconLoader by lazy { BeaconLoader(beaconService, prefs.beacons) }
    private var sort = BeaconSortMethod.Closest

    private val listMapper by lazy {
        IBeaconListItemMapper(
            requireContext(),
            gps,
            this::handleBeaconAction,
            this::handleBeaconGroupAction
        )
    }

    private var initialLocation: GeoUri? = null

    private val gpxService by lazy {
        IOFactory().createGpxService(this)
    }

    private lateinit var manager: GroupListManager<IBeacon>
    private var lastRoot: IBeacon? = null

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val views = createBeaconListViews(requireContext())
        beaconViews = views
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    BeaconListHost(
                        views = views,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (requireArguments().containsKey("initial_location")) {
            initialLocation = BundleCompat.getParcelable(
                requireArguments(), "initial_location",
                GeoUri::class.java
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = findNavController()

        sort = prefs.beacons.beaconSort

        binding.beaconRecycler.emptyView = binding.beaconEmptyText
        manager = GroupListManager(
            lifecycleScope,
            beaconLoader,
            lastRoot,
            this::sortBeacons
        )

        manager.bind(binding.searchbox)
        manager.bind(binding.beaconRecycler, binding.beaconTitle.title, listMapper) {
            it?.name ?: getString(R.string.beacons)
        }

        initialLocation?.let {
            initialLocation = null
            createBeacon(initialLocation = it)
        }

        binding.beaconTitle.rightButton.setOnClickListener {
            val defaultSort = prefs.beacons.beaconSort
            Pickers.menu(
                it, listOf(
                    getString(R.string.sort_by, getSortString(defaultSort)),
                    getString(R.string.export),
                    getString(R.string.navigate_to_nearest_cell_signal),
                    getString(R.string.delete)
                )
            ) { selected ->
                when (selected) {
                    0 -> changeSort()
                    1 -> onExportBeacons()
                    2 -> navigateToNearestCellSignal()
                    3 -> deleteCurrentGroup()
                }
                true
            }
        }

        binding.createMenu.setOverlay(binding.overlayMask)
        binding.createMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_import_qr_beacon -> {
                    requestCamera { hasPermission ->
                        if (hasPermission) {
                            importBeaconFromQR()
                        } else {
                            alertNoCameraPermission()
                        }
                    }
                    setCreateMenuVisibility(false)
                }

                R.id.action_import_gpx_beacons -> {
                    importBeacons()
                    setCreateMenuVisibility(false)
                }

                R.id.action_create_beacon_group -> {
                    val command =
                        CreateBeaconGroupCommand(requireContext(), lifecycleScope, beaconService) {
                            refresh()
                        }
                    command.execute(manager.root?.id)
                    setCreateMenuVisibility(false)
                }

                R.id.action_create_beacon -> {
                    setCreateMenuVisibility(false)
                    createBeacon(group = manager.root?.id)
                }
            }
            true
        }
        binding.createMenu.setOnHideListener {
            binding.createBtn.setImageResource(R.drawable.ic_add)
        }

        binding.createMenu.setOnShowListener {
            binding.createBtn.setImageResource(R.drawable.ic_cancel)
        }

        binding.createBtn.setOnClickListener {
            setCreateMenuVisibility(!isCreateMenuOpen())
        }

        onBackPressed {
            when {
                isCreateMenuOpen() -> {
                    setCreateMenuVisibility(false)
                }

                else -> {
                    if (!manager.up()) {
                        remove()
                        navController.navigateUp()
                    }
                }
            }
        }
    }

    private suspend fun sortBeacons(beacons: List<IBeacon>): List<IBeacon> {
        val method = when (sort) {
            BeaconSortMethod.MostRecent -> MostRecentBeaconSort(beaconService)
            BeaconSortMethod.Closest -> ClosestBeaconSort(beaconService, gps::location)
            BeaconSortMethod.Name -> NameBeaconSort()
        }
        return method.sort(beacons)
    }

    private fun setCreateMenuVisibility(isShowing: Boolean) {
        if (isShowing) {
            binding.createMenu.show()
        } else {
            binding.createMenu.hide()
        }
    }

    private fun isCreateMenuOpen(): Boolean {
        return binding.createMenu.isVisible
    }

    private fun onExportBeacons() {
        inBackground {
            val gpx = getExportGPX()
            onMain {
                Pickers.items(
                    requireContext(),
                    getString(R.string.export),
                    gpx.waypoints.map { it.name ?: formatService.formatLocation(it.coordinate) },
                    List(gpx.waypoints.size) { it },
                ) {
                    if (!it.isNullOrEmpty()) {
                        val selectedWaypoints = gpx.waypoints.filterIndices(it)
                        export(gpx.copy(waypoints = selectedWaypoints))
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        manager.refresh()
        // Get a GPS reading
        gps.start(this::onLocationUpdate)
    }

    override fun onPause() {
        gps.stop(this::onLocationUpdate)
        tryOrNothing {
            lastRoot = manager.root
        }
        super.onPause()
    }

    override fun onDestroyView() {
        beaconViews = null
        super.onDestroyView()
    }

    private fun onLocationUpdate(): Boolean {
        return false
    }

    private fun importBeaconFromQR() {
        val encoder = BeaconQREncoder()
        CustomUiUtils.scanQR(this, getString(R.string.beacon_qr_import_instructions)) {
            if (it != null) {
                val beacon = encoder.decode(it) ?: return@scanQR true
                createBeacon(group = manager.root?.id, initialLocation = GeoUri.from(beacon))
            }
            false
        }
    }

    private fun changeSort() {
        val sortOptions = BeaconSortMethod.values()
        Pickers.item(
            requireContext(),
            getString(R.string.sort),
            sortOptions.map { getSortString(it) },
            sortOptions.indexOf(prefs.beacons.beaconSort)
        ) { newSort ->
            if (newSort != null) {
                prefs.beacons.beaconSort = sortOptions[newSort]
                sort = sortOptions[newSort]
                onSortChanged()
            }
        }
    }

    private fun getSortString(sortMethod: BeaconSortMethod): String {
        return when (sortMethod) {
            BeaconSortMethod.MostRecent -> getString(R.string.most_recent)
            BeaconSortMethod.Closest -> getString(R.string.closest)
            BeaconSortMethod.Name -> getString(R.string.name)
        }
    }

    private fun onSortChanged() {
        manager.refresh(true)
    }

    private fun export(gpx: GPXData) {
        inBackground(BackgroundMinimumState.Created) {
            val exportFile = "trail-sense-${Instant.now().epochSecond}.gpx"
            val success = gpxService.export(gpx, exportFile)
            onMain {
                if (success) {
                    toast(
                        resources.getQuantityString(
                            R.plurals.beacons_exported,
                            gpx.waypoints.size,
                            gpx.waypoints.size
                        )
                    )
                } else {
                    toast(
                        getString(R.string.beacon_export_error)
                    )
                }
            }
        }
    }

    private fun importBeacons() {
        val importer = BeaconGpxImporter(requireContext())
        inBackground(BackgroundMinimumState.Created) {
            val gpx = gpxService.import()
            val waypoints = gpx?.waypoints ?: emptyList()
            onMain {
                Pickers.items(
                    requireContext(),
                    getString(R.string.import_btn),
                    waypoints.map {
                        it.name ?: formatService.formatLocation(it.coordinate)
                    },
                    List(waypoints.size) { it }
                ) {
                    if (it != null) {
                        inBackground {
                            val gpxToImport =
                                GPXData(waypoints.filterIndices(it), emptyList(), emptyList())
                            val count = onIO {
                                importer.import(gpxToImport, manager.root?.id)
                            }
                            onMain {
                                Alerts.toast(
                                    requireContext(),
                                    resources.getQuantityString(
                                        R.plurals.beacons_imported,
                                        count,
                                        count
                                    )
                                )
                                refresh()
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun getExportGPX(): GPXData = onIO {
        val all = beaconService.getBeacons(
            manager.root?.id,
            includeGroups = true,
            maxDepth = null,
            includeRoot = true
        )

        val groups = all.filterIsInstance<BeaconGroup>()
        val beacons = all.filterIsInstance<Beacon>()

        BeaconGpxConverter().toGPX(beacons, groups)
    }

    private fun createBeacon(
        group: Long? = null,
        initialLocation: GeoUri? = null,
        editingBeaconId: Long? = null
    ) {
        val bundle = Bundle()

        group?.let { bundle.putLong("initial_group", it) }
        initialLocation?.let { bundle.putParcelable("initial_location", it) }
        editingBeaconId?.let { bundle.putLong("edit_beacon", it) }

        navController.navigate(
            R.id.action_beaconListFragment_to_placeBeaconFragment,
            bundle
        )
    }

    fun refresh() {
        manager.refresh()
    }

    private fun viewBeacon(id: Long) {
        val bundle = Bundle().apply {
            putLong("beacon_id", id)
        }
        navController.navigate(
            R.id.action_beacon_list_to_beaconDetailsFragment,
            bundle
        )
    }

    private fun navigate(beacon: Beacon) {
        val navigator = Navigator.getInstance(requireContext())
        navigator.navigateTo(beacon)
        navController.navigate(R.id.action_navigation)
    }

    private fun deleteCurrentGroup() {
        val group = manager.root
        delete(group as BeaconGroup?)
    }

    private fun delete(beacon: Beacon) {
        val command = DeleteBeaconCommand(
            requireContext(),
            lifecycleScope,
            beaconService
        ) {
            refresh()
        }
        command.execute(beacon)
    }

    private fun delete(group: BeaconGroup?) {
        Alerts.dialog(
            requireContext(),
            getString(R.string.delete),
            group?.name?.let {
                getString(
                    R.string.delete_beacon_group_message,
                    it
                )
            } ?: getString(R.string.delete_all_beacons),
        ) { cancelled ->
            if (!cancelled) {
                inBackground {
                    onIO {
                        beaconService.delete(group)
                    }

                    onMain {
                        if (group == manager.root) {
                            manager.up()
                        }
                        refresh()
                    }
                }

            }
        }
    }

    private fun move(beacon: Beacon) {
        val command = MoveBeaconCommand(
            requireContext(),
            lifecycleScope,
            beaconService
        ) {
            refresh()
        }
        command.execute(beacon)
    }

    private fun move(group: BeaconGroup) {
        val command =
            MoveBeaconGroupCommand(requireContext(), lifecycleScope, beaconService) { refresh() }
        command.execute(group)
    }

    private fun toggleVisibility(beacon: Beacon) {
        inBackground {
            val newBeacon = beacon.copy(visible = !beacon.visible)

            onIO {
                beaconService.add(newBeacon)
            }

            onMain {
                refresh()
            }
        }
    }

    private fun setGroupVisibility(group: BeaconGroup, visible: Boolean) {
        inBackground {
            Alerts.withLoading(requireContext(), getString(R.string.loading)) {
                val beacons = onIO {
                    beaconService.loader.getChildren(group.id, null).filterIsInstance<Beacon>()
                }

                onIO {
                    beacons
                        .asSequence()
                        .filter { it.visible != visible }
                        .forEach { beaconService.add(it.copy(visible = visible)) }
                }

                onMain {
                    refresh()
                }
            }
        }
    }

    private fun rename(group: BeaconGroup) {
        val command =
            RenameBeaconGroupCommand(requireContext(), lifecycleScope, beaconService) { refresh() }
        command.execute(group)
    }

    private fun navigateToNearestCellSignal() {
        inBackground {
            val command = NavigateToNearestCellSignal(requireContext())
            if (command.execute()) {
                onMain {
                    findNavController().navigate(R.id.action_navigation)
                }
            } else {
                onMain {
                    toast(getString(R.string.no_recorded_cell_signal_nearby))
                }
            }
        }
    }

    private fun handleBeaconGroupAction(group: BeaconGroup, action: BeaconGroupAction) {
        when (action) {
            BeaconGroupAction.Open -> {
                manager.open(group.id)
            }

            BeaconGroupAction.ShowAll -> setGroupVisibility(group, true)
            BeaconGroupAction.HideAll -> setGroupVisibility(group, false)
            BeaconGroupAction.Edit -> rename(group)
            BeaconGroupAction.Delete -> delete(group)
            BeaconGroupAction.Move -> move(group)
        }
    }

    private fun handleBeaconAction(beacon: Beacon, action: BeaconAction) {
        when (action) {
            BeaconAction.View -> viewBeacon(beacon.id)
            BeaconAction.Edit -> createBeacon(editingBeaconId = beacon.id)
            BeaconAction.Navigate -> navigate(beacon)
            BeaconAction.Delete -> delete(beacon)
            BeaconAction.Move -> move(beacon)
            BeaconAction.ToggleVisibility -> toggleVisibility(beacon)
            BeaconAction.Share -> BeaconSender(this).send(beacon)
        }
    }
}

private data class BeaconListViews(
    val root: View,
    val beaconTitle: BeaconListTitle,
    val searchbox: SearchView,
    val beaconRecycler: AndromedaListView,
    val beaconEmptyText: TextView,
    val overlayMask: ImageView,
    val createMenu: FloatingActionButtonMenu,
    val createBtn: FloatingActionButton
)

private data class BeaconListTitle(
    val root: View,
    val title: TextView,
    val rightButton: ImageButton
)

@Composable
private fun BeaconListHost(
    views: BeaconListViews,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

private fun createBeaconListViews(context: Context): BeaconListViews {
    val root = FrameLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }
    root.addView(
        content,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )

    val title = createBeaconListTitle(context)
    content.addView(
        title.root,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    val searchbox = SearchView(context, null).apply {
        id = R.id.searchbox
    }
    content.addView(
        searchbox,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(16.dp(context), 0, 16.dp(context), 0)
        }
    )

    val list = AndromedaListView(context, null).apply {
        id = R.id.beacon_recycler
    }
    content.addView(
        list,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ).apply {
            topMargin = 16.dp(context)
        }
    )

    val emptyText = TextView(context).apply {
        id = R.id.beacon_empty_text
        text = context.getString(R.string.no_beacons)
        textSize = 18f
        visibility = View.GONE
        setTextColor(Resources.androidTextColorPrimary(context))
    }
    root.addView(
        emptyText,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
    )

    val overlay = ImageView(context).apply {
        id = R.id.overlay_mask
        alpha = 0.86f
        setBackgroundColor(resolveThemeColor(context, android.R.attr.colorBackground))
        isClickable = true
        visibility = View.GONE
    }
    root.addView(
        overlay,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )

    val createBtn = FloatingActionButton(context).apply {
        id = R.id.create_btn
        setImageResource(R.drawable.ic_add)
        isClickable = true
        isFocusable = true
    }
    val bottomMargin = context.resources.getDimensionPixelSize(R.dimen.default_bottom_margin)
    root.addView(
        createBtn,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.END
        ).apply {
            marginEnd = 16.dp(context)
            this.bottomMargin = bottomMargin
        }
    )

    val createMenu = FloatingActionButtonMenu(context, null).apply {
        id = R.id.create_menu
        visibility = View.GONE
        setMenu(R.menu.beacon_create_menu)
    }
    root.addView(
        createMenu,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.END
        ).apply {
            marginEnd = 16.dp(context)
            this.bottomMargin = bottomMargin + 72.dp(context)
        }
    )

    return BeaconListViews(
        root = root,
        beaconTitle = title,
        searchbox = searchbox,
        beaconRecycler = list,
        beaconEmptyText = emptyText,
        overlayMask = overlay,
        createMenu = createMenu,
        createBtn = createBtn
    )
}

private fun createBeaconListTitle(context: Context): BeaconListTitle {
    val root = LinearLayout(context).apply {
        id = R.id.beacon_title
        gravity = Gravity.CENTER_VERTICAL
        orientation = LinearLayout.HORIZONTAL
        setPadding(16.dp(context), 8.dp(context), 8.dp(context), 8.dp(context))
    }
    val title = TextView(context).apply {
        text = context.getString(R.string.beacons)
        textSize = 20f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Resources.androidTextColorPrimary(context))
    }
    val menu = ImageButton(context).apply {
        id = R.id.beacon_list_menu_btn
        setImageResource(R.drawable.ic_menu_dots)
        backgroundTintList = ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
        imageTintList = ColorStateList.valueOf(Resources.androidTextColorSecondary(context))
    }
    root.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    root.addView(menu, LinearLayout.LayoutParams(48.dp(context), 48.dp(context)))

    return BeaconListTitle(root, title, menu)
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
private fun BeaconListHostPreview() {
    TrailSenseTheme {
        BeaconListHost(
            views = createBeaconListViews(androidx.compose.ui.platform.LocalContext.current)
        )
    }
}
