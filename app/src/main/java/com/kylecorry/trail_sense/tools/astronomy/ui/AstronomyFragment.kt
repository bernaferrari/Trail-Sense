package com.kylecorry.trail_sense.tools.astronomy.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.text.capitalizeWords
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.luna.text.capitalizeWords
import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.sol.time.Time.atEndOfDay
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.ErrorBannerReason
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.gps.MockedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.views.DatePickerContent
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.UserError
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAndromedaComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.astronomy.domain.AstroPositions
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyEvent
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.astronomy.quickactions.AstronomyQuickActionBinder
import com.kylecorry.trail_sense.tools.astronomy.ui.commands.AstroChartData
import com.kylecorry.trail_sense.tools.astronomy.ui.commands.CenteredAstroChartDataProvider
import com.kylecorry.trail_sense.tools.astronomy.ui.commands.DailyAstroChartDataProvider
import com.kylecorry.trail_sense.tools.astronomy.ui.items.AstronomyListItem
import com.kylecorry.trail_sense.tools.astronomy.ui.items.AstronomyListItemData
import com.kylecorry.trail_sense.tools.astronomy.ui.items.LunarEclipseListItemProducer
import com.kylecorry.trail_sense.tools.astronomy.ui.items.MeteorShowerListItemProducer
import com.kylecorry.trail_sense.tools.astronomy.ui.items.MoonListItemProducer
import com.kylecorry.trail_sense.tools.astronomy.ui.items.SolarEclipseListItemProducer
import com.kylecorry.trail_sense.tools.astronomy.ui.items.SunListItemProducer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.ARMode
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityFragment
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class AstronomyFragment : TrailSenseAndromedaComposeFragment() {

    private var astronomyViews: AstronomyViews? = null
    private val views: AstronomyViews
        get() = astronomyViews ?: error("Astronomy views are not ready")
    private val isViewReady: Boolean
        get() = astronomyViews != null && view != null

    private lateinit var gps: IGPS

    private lateinit var chart: AstroChart

    private lateinit var sunTimesMode: SunTimesMode

    private val sensorService by lazy { SensorService(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val astronomyService = AstronomyService()
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val declination by lazy { DeclinationFactory().getDeclinationStrategy(prefs, gps) }
    private val markdownService by lazy { DependencyRegistry.get<MarkdownService>() }

    private var lastAstronomyEventSearch: AstronomyEvent? = null

    private var minChartTime = ZonedDateTime.now()
    private var maxChartTime = ZonedDateTime.now()
    private val maxProgress = 60 * 24

    private var gpsErrorShown = false

    private val periodicUpdater = CoroutineTimer {
        refreshAstronomy()
    }

    private var displayDate = LocalDate.now()
    private var location = Coordinate.zero
    private var currentSeekChartTime = ZonedDateTime.now()
    private var isSeeking = false
    private var currentMoonTilt = 0f

    private val astroChartDataProvider by lazy {
        if (prefs.astronomy.centerSunAndMoon) {
            CenteredAstroChartDataProvider()
        } else {
            DailyAstroChartDataProvider()
        }
    }

    private var data = AstroChartData(emptyList(), emptyList())
    private var astronomyItems by mutableStateOf<List<AstronomyListItem>>(emptyList())

    private val producers by lazy {
        listOf(
            SunListItemProducer(requireContext()),
            MoonListItemProducer(requireContext()),
            MeteorShowerListItemProducer(requireContext()),
            LunarEclipseListItemProducer(requireContext()),
            SolarEclipseListItemProducer(requireContext())
        )
    }

    override fun beforeCreateContent() {
        val views = createAstronomyViews(requireContext())
        astronomyViews = views
        views.astronomyDetailList.setContent {
            TrailSenseTheme {
                AstronomyDetailList(
                    items = astronomyItems,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    @Composable
    override fun FragmentContent() {
        AstronomyHost(
            views = views,
            modifier = Modifier.fillMaxSize()
        )
    }

    override fun onDestroyView() {
        astronomyViews = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        AstronomyQuickActionBinder(
            this, views.astronomyTitle, prefs.astronomy
        ).bind()

        chart = AstroChart(views.sunMoonChart, this::showTimeSeeker)

        views.button3d.isVisible =
            Tools.isToolAvailable(requireContext(), Tools.AUGMENTED_REALITY)
        views.button3d.setOnClickListener {
            val time = if (isSeeking) {
                currentSeekChartTime
            } else if (views.displayDate.date == LocalDate.now()) {
                ZonedDateTime.now()
            } else {
                views.displayDate.date.atTime(ZonedDateTime.now().toLocalTime()).toZonedDateTime()
            }
            AugmentedRealityFragment.open(
                findNavController(),
                ARMode.Astronomy,
                enableCamera = prefs.astronomy.startCameraIn3DView,
                Bundle().apply {
                    putString("time", time.toString())
                }
            )
        }

        views.displayDate.setOnDateChangeListener {
            displayDate = it
            refreshAstronomy()
        }

        views.displayDate.searchEnabled = true
        views.displayDate.setOnSearchListener {
            val options = listOfNotNull(
                AstronomyEvent.FullMoon,
                AstronomyEvent.NewMoon,
                AstronomyEvent.QuarterMoon,
                AstronomyEvent.MeteorShower,
                AstronomyEvent.LunarEclipse,
                AstronomyEvent.SolarEclipse,
                AstronomyEvent.Supermoon
            )

            val optionNames = listOfNotNull(
                getString(R.string.full_moon),
                getString(R.string.new_moon),
                getString(R.string.quarter_moon),
                getString(R.string.meteor_shower),
                getString(R.string.lunar_eclipse),
                getString(R.string.solar_eclipse),
                getString(R.string.supermoon)
            ).map { it.capitalizeWords() }

            Pickers.item(
                requireContext(),
                getString(R.string.find_next_occurrence),
                optionNames,
                options.indexOf(lastAstronomyEventSearch)
            ) {
                if (it != null) {
                    val search = options[it]
                    lastAstronomyEventSearch = search
                    val currentDate = views.displayDate.date
                    inBackground {
                        val job = launch {
                            val nextEvent = astronomyService.findNextEvent(
                                search, location, currentDate
                            )
                            onMain {
                                val newDate = nextEvent ?: currentDate
                                displayDate = newDate
                                views.displayDate.updateDate(newDate)
                                refreshAstronomy()

                                if (nextEvent == null) {
                                    toast(
                                        getString(
                                            R.string.unable_to_find_next_astronomy,
                                            optionNames[it].lowercase()
                                        )
                                    )
                                }
                            }
                        }

                        Alerts.withCancelableLoading(
                            requireContext(),
                            getString(R.string.loading),
                            onCancel = { job.cancel() }) {
                            job.join()
                        }
                    }
                }
            }
        }

        gps = sensorService.getGPS()

        sunTimesMode = prefs.astronomy.sunTimesMode

        views.timeSeeker.valueFrom = 0f
        views.timeSeeker.valueTo = maxProgress.toFloat()
        views.timeSeeker.labelBehavior = LabelFormatter.LABEL_GONE

        views.timeSeeker.addOnChangeListener { _, value, _ ->
            val progress = value.toInt()
            val seconds = (Duration.between(
                minChartTime, maxChartTime
            ).seconds * progress / maxProgress.toFloat()).toLong()
            currentSeekChartTime = minChartTime.plusSeconds(seconds)
            refreshSeekDetails()
        }

        refreshAstronomy()
    }

    private fun showTimeSeeker(time: ZonedDateTime?) {
        if (isSeeking && time == null) {
            return
        }
        isSeeking = true
        views.timeSeekerPanel.isVisible = true
        views.astronomyDetailList.isVisible = false
        views.button3d.isVisible = false
        currentSeekChartTime = time ?: ZonedDateTime.now()
        views.seekTime.text =
            formatService.formatTime(currentSeekChartTime.toLocalTime(), includeSeconds = false)

        views.timeSeeker.value = getSeekProgress().toFloat()

        views.closeSeek.setOnClickListener {
            hideTimeSeeker()
        }
    }

    private fun updateSeekPositions() {
        val positions = getSunMoonPositions(currentSeekChartTime)
        views.sunPositionText.text = markdownService.toMarkdown(
            getString(
                R.string.sun_moon_position_template,
                getString(R.string.sun),
                formatService.formatDegrees(positions.sunAltitude),
                formatService.formatDegrees(positions.sunAzimuth)
            )
        )

        views.moonPositionText.text = markdownService.toMarkdown(
            getString(
                R.string.sun_moon_position_template,
                getString(R.string.moon),
                formatService.formatDegrees(positions.moonAltitude),
                formatService.formatDegrees(positions.moonAzimuth)
            )
        )
    }

    private fun getSeekProgress(): Int {
        val totalDuration = Duration.between(minChartTime, maxChartTime).seconds
        val currentDuration = Duration.between(minChartTime, currentSeekChartTime).seconds
        val progress = maxProgress * currentDuration / totalDuration.toFloat()
        return progress.toInt()
    }

    private fun hideTimeSeeker() {
        isSeeking = false
        views.timeSeekerPanel.isVisible = false
        views.astronomyDetailList.isVisible = true
        views.button3d.isVisible =
            Tools.isToolAvailable(requireContext(), Tools.AUGMENTED_REALITY)
        val displayDate = views.displayDate.date
        if (displayDate == LocalDate.now()) {
            plotMoonImage(data.moon)
            plotSunImage(data.sun)
        } else {
            chart.moveSun(null)
            chart.moveMoon(null)
        }
    }

    private fun getSunMoonPositions(time: ZonedDateTime): AstroPositions {
        val moonPosition = astronomyService.getMoonPosition(location, time)
        val sunPosition = astronomyService.getSunPosition(location, time)
        val declination = if (!prefs.compass.useTrueNorth) getDeclination() else 0f

        val sunAzimuth =
            sunPosition.azimuth.withDeclination(-declination).value
        val moonAzimuth =
            moonPosition.azimuth.withDeclination(-declination).value

        return AstroPositions(
            moonPosition.altitude,
            sunPosition.altitude,
            moonAzimuth,
            sunAzimuth
        )
    }

    override fun onResume() {
        super.onResume()
        chart.setBands(prefs.astronomy.showAstronomyBands)
        views.displayDate.reset()
        requestLocationUpdate()
        views.displayDate.setDayViewDecorator(AstronomyDayViewDecorator(location))
        detectAndShowGPSError()
        periodicUpdater.interval(60000)
        refreshAstronomy()

        CustomUiUtils.oneTimeToast(
            requireContext(),
            getString(R.string.tap_sun_moon_hint),
            "cache_tap_sun_moon_shown"
        )
    }

    override fun onPause() {
        super.onPause()
        gps.stop(this::onLocationUpdate)
        periodicUpdater.stop()
        gpsErrorShown = false
    }

    private fun requestLocationUpdate() {
        location = gps.location
        refreshAstronomy()
        if (gps.hasValidReading) {
            onLocationUpdate()
        } else {
            gps.start(this::onLocationUpdate)
        }
    }

    private fun onLocationUpdate(): Boolean {
        location = gps.location
        refreshAstronomy()
        return false
    }

    private fun getDeclination(): Float {
        return declination.getDeclination()
    }

    private suspend fun updateMoonUI() {
        if (!isViewReady) {
            return
        }

        val displayDate = views.displayDate.date

        val moonPhase = withContext(Dispatchers.Default) {
            if (displayDate == LocalDate.now()) {
                astronomyService.getCurrentMoonPhase()
            } else {
                astronomyService.getMoonPhase(displayDate)
            }
        }

        val moonPhase = onDefault { astronomyService.getMoonPhase(time) }
        val tilt = onDefault { astronomyService.getMoonTilt(location, time) }

        withContext(Dispatchers.Main) {
            val size = Resources.dp(requireContext(), 24f).toInt()
            chart.setMoonImage(
                MoonPhaseImageMapper(requireContext()).getPhaseImage(
                    moonPhase.phaseAngle,
                    size,
                    size,
                    tilt
                )
            )
        }
    }

    private suspend fun updateAstronomyChart() {
        val displayDate = views.displayDate.date

        data = onDefault {
            val time =
                if (displayDate == LocalDate.now()) ZonedDateTime.now() else displayDate.atStartOfDay()
                    .toZonedDateTime()
            astroChartDataProvider.get(location, time)
        }

        minChartTime = data.sun.first().time.toZonedDateTime()
        maxChartTime = data.sun.last().time.toZonedDateTime()

        onMain {
            chart.plot(data.sun, data.moon)

            if (displayDate == LocalDate.now()) {
                plotMoonImage(data.moon)
                plotSunImage(data.sun)
            } else {
                chart.moveSun(null)
                chart.moveMoon(null)
            }
        }
    }

    private fun plotSunImage(
        altitudes: List<Reading<Float>>, time: ZonedDateTime = ZonedDateTime.now()
    ) {
        val instant = time.toInstant()
        val current = altitudes.minByOrNull {
            Duration.between(instant, it.time).abs()
        }
        chart.moveSun(current)
    }

    private fun plotMoonImage(
        altitudes: List<Reading<Float>>,
        time: ZonedDateTime = ZonedDateTime.now(),
    ) {
        val instant = time.toInstant()
        val current = altitudes.minByOrNull {
            Duration.between(instant, it.time).abs()
        }
        chart.moveMoon(current)
    }

    private suspend fun updateAstronomyDetails() {
        if (!isViewReady) {
            return
        }

        val displayDate = views.displayDate.date

        onDefault {
            val declination = if (!prefs.compass.useTrueNorth) getDeclination() else 0f

            val items = producers.map { it.getListItem(displayDate, location, declination) }

            onMain {
                astronomyItems = items.filterNotNull()
            }

        }
    }


    private suspend fun displayTimeUntilNextSunEvent() {
        val currentTime = LocalDateTime.now()

        var nextSunrise: LocalDateTime?
        var nextSunset: LocalDateTime?
        withContext(Dispatchers.Default) {
            nextSunrise = astronomyService.getNextSunrise(location, sunTimesMode)
            nextSunset = astronomyService.getNextSunset(location, sunTimesMode)
        }

        withContext(Dispatchers.Main) {
            if (nextSunrise != null && (nextSunset == null || nextSunrise.isBefore(nextSunset))) {
                views.astronomyTitle.title.text =
                    formatService.formatDuration(Duration.between(currentTime, nextSunrise))
                views.astronomyTitle.subtitle.text = getString(R.string.until_sunrise)
            } else if (nextSunset != null) {
                views.astronomyTitle.title.text =
                    formatService.formatDuration(Duration.between(currentTime, nextSunset))
                views.astronomyTitle.subtitle.text = getString(R.string.until_sunset)
            } else if (astronomyService.isSunUp(location)) {
                views.astronomyTitle.title.text = getString(R.string.sun_up_no_set)
                views.astronomyTitle.subtitle.text = getString(R.string.sun_does_not_set)
            } else {
                views.astronomyTitle.title.text = getString(R.string.sun_down_no_set)
                views.astronomyTitle.subtitle.text = getString(R.string.sun_does_not_rise)
            }
        }
    }

    private fun detectAndShowGPSError() {
        if (gpsErrorShown) {
            return
        }

        if (gps is MockedGPS && prefs.locationOverride == Coordinate.zero) {
            val activity = requireActivity() as MainActivity
            val navController = findNavController()
            val error = UserError(
                ErrorBannerReason.LocationNotSet,
                getString(R.string.location_not_set),
                R.drawable.satellite,
                getString(R.string.set)
            ) {
                activity.errorBanner.dismiss(ErrorBannerReason.LocationNotSet)
                navController.navigate(R.id.calibrateGPSFragment)
            }
            activity.errorBanner.report(error)
            gpsErrorShown = true
        } else if (gps is CachedGPS && location == Coordinate.zero) {
            val error = UserError(
                ErrorBannerReason.NoGPS, getString(R.string.location_disabled), R.drawable.satellite
            )
            (requireActivity() as MainActivity).errorBanner.report(error)
            gpsErrorShown = true
        }
    }

    private fun refreshSeekDetails() {
        if (!isSeeking || !isViewReady) {
            return
        }
        updateSeekPositions()
        views.seekTime.text = formatService.formatTime(
            currentSeekChartTime.toLocalTime(),
            includeSeconds = false
        )
        if (data.moon.isNotEmpty() && data.sun.isNotEmpty()) {
            val tilt = astronomyService.getMoonTilt(location, currentSeekChartTime)
            plotMoonImage(data.moon, currentSeekChartTime, tilt)
            plotSunImage(data.sun, currentSeekChartTime)
        }
    }

    private fun refreshAstronomy() {
        if (!isViewReady || !this::gps.isInitialized) {
            return
        }
        detectAndShowGPSError()
        views.displayDate.setDayViewDecorator(AstronomyDayViewDecorator(location))
        inBackground {
            updateAstronomyDetails()
            updateAstronomyChart()
            displayTimeUntilNextSunEvent()
            updateMoonUI()
            onMain {
                refreshSeekDetails()
            }
        }
    }

}

private data class AstronomyViews(
    val root: View,
    val astronomyTitle: Toolbar,
    val displayDate: AstronomyDatePickerState,
    val sunMoonChart: Chart,
    val timeSeekerPanel: ScrollView,
    val seekTime: TextView,
    val closeSeek: ImageButton,
    val timeSeeker: Slider,
    val sunPositionText: TextView,
    val moonPositionText: TextView,
    val astronomyDetailList: ComposeView,
    val button3d: Button
)

private class AstronomyDatePickerState(context: Context) {
    private val formatter = FormatService.getInstance(context)
    private val pickerContext = context
    private var dayViewDecorator: AstronomyDayViewDecorator? = null
    private var onDateChange: (LocalDate) -> Unit = {}
    private var onSearch: () -> Unit = {}
    private var onCalendarLongPress: () -> Unit = {
        date = LocalDate.now()
    }

    var date by mutableStateOf(LocalDate.now())
        private set

    var searchEnabled by mutableStateOf(false)

    fun updateDate(value: LocalDate) {
        val changed = date != value
        date = value
        if (changed) {
            onDateChange(value)
        }
    }

    fun previous() {
        updateDate(date.minusDays(1))
    }

    fun next() {
        updateDate(date.plusDays(1))
    }

    fun pickDate() {
        CustomUiUtils.pickDate(pickerContext, date, dayViewDecorator) {
            if (it != null) {
                updateDate(it)
            }
        }
    }

    fun reset() {
        updateDate(LocalDate.now())
    }

    fun setOnDateChangeListener(listener: (LocalDate) -> Unit) {
        onDateChange = listener
    }

    fun setOnCalendarLongPressListener(listener: () -> Unit) {
        onCalendarLongPress = listener
    }

    fun setOnSearchListener(listener: () -> Unit) {
        onSearch = listener
    }

    fun setDayViewDecorator(decorator: AstronomyDayViewDecorator?) {
        dayViewDecorator = decorator
    }

    @Composable
    fun Content() {
        DatePickerContent(
            date = date,
            formatter = formatter,
            searchEnabled = searchEnabled,
            onPrevious = ::previous,
            onNext = ::next,
            onPickDate = ::pickDate,
            onCalendarLongPress = onCalendarLongPress,
            onSearch = onSearch
        )
    }
}

@Composable
private fun AstronomyHost(
    views: AstronomyViews,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

@Composable
private fun AstronomyDetailList(
    items: List<AstronomyListItem>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.testTag("astronomy_detail_list")
    ) {
        items(items, key = { it.id }) { item ->
            AstronomyDetailRow(item)
            TrailSenseDivider()
        }
    }
}

@Composable
private fun AstronomyDetailRow(item: AstronomyListItem) {
    TrailSenseListItem(
        title = item.title,
        subtitle = item.subtitle,
        icon = item.icon?.let { painterResource(it.resource) },
        iconTint = item.icon?.tint?.let { Color(it) } ?: MaterialTheme.colorScheme.primary,
        iconRotation = item.icon?.rotation ?: 0f,
        trailing = {
            AstronomyDataRow(item.data)
        },
        onClick = item.onClick,
        animateEntrance = false,
        modifier = Modifier
            .padding(horizontal = TrailSenseSpacing.lg)
            .testTag("astronomy_detail_title")
    )
}

@Composable
private fun AstronomyDataRow(data: List<AstronomyListItemData>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        data.forEach {
            Text(
                text = if (it.label == null) {
                    it.value
                } else {
                    "${it.value}\n${it.label}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("astronomy_detail_data")
            )
        }
    }
}

private fun createAstronomyViews(context: Context): AstronomyViews {
    val root = ConstraintLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val title = Toolbar(context, null).apply {
        id = R.id.astronomy_title
    }
    root.addView(
        title,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )

    val displayDate = AstronomyDatePickerState(context)
    val displayDateView = ComposeView(context).apply {
        id = R.id.display_date
        setContent {
            TrailSenseTheme {
                displayDate.Content()
            }
        }
    }
    root.addView(
        displayDateView,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToBottom = R.id.astronomy_title
            marginStart = 16.dp(context)
            marginEnd = 16.dp(context)
            topMargin = 16.dp(context)
        }
    )

    val chart = Chart(context).apply {
        id = R.id.sunMoonChart
    }
    root.addView(
        chart,
        ConstraintLayout.LayoutParams(
            0,
            context.resources.getDimensionPixelSize(R.dimen.chart_height)
        ).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToBottom = R.id.display_date
            marginStart = 16.dp(context)
            marginEnd = 16.dp(context)
        }
    )

    val timeSeekerPanel = ScrollView(context).apply {
        id = R.id.time_seeker_panel
        visibility = View.GONE
    }
    root.addView(
        timeSeekerPanel,
        ConstraintLayout.LayoutParams(0, 0).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToBottom = R.id.sunMoonChart
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            topMargin = 8.dp(context)
        }
    )

    val seekContent = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }
    timeSeekerPanel.addView(
        seekContent,
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    val seekHeader = ConstraintLayout(context)
    seekContent.addView(
        seekHeader,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    val seekTime = TextView(context).apply {
        id = R.id.seek_time
        textAlignment = View.TEXT_ALIGNMENT_CENTER
        textSize = 18f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
    }
    seekHeader.addView(
        seekTime,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            marginStart = 16.dp(context)
            marginEnd = 16.dp(context)
        }
    )

    val closeSeek = ImageButton(context).apply {
        id = R.id.close_seek
        setImageResource(R.drawable.ic_cancel)
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        imageTintList = android.content.res.ColorStateList.valueOf(Resources.androidTextColorSecondary(context))
    }
    seekHeader.addView(
        closeSeek,
        ConstraintLayout.LayoutParams(32.dp(context), 32.dp(context)).apply {
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            marginEnd = 16.dp(context)
        }
    )

    val timeSeeker = Slider(context).apply {
        id = R.id.time_seeker
    }
    seekContent.addView(
        timeSeeker,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 16.dp(context)
        }
    )

    val positions = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
    }
    seekContent.addView(
        positions,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = 16.dp(context)
            marginEnd = 16.dp(context)
            topMargin = 8.dp(context)
        }
    )

    val sunPosition = TextView(context).apply {
        id = R.id.sun_position_text
        setLineSpacing(0f, 0.8f)
    }
    positions.addView(
        sunPosition,
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    )

    val moonPosition = TextView(context).apply {
        id = R.id.moon_position_text
        setLineSpacing(0f, 0.8f)
    }
    positions.addView(
        moonPosition,
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    )

    val list = ComposeView(context).apply {
        id = R.id.astronomy_detail_list
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }
    root.addView(
        list,
        ConstraintLayout.LayoutParams(0, 0).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToBottom = R.id.sunMoonChart
            bottomToTop = R.id.button_3d
            topMargin = 16.dp(context)
        }
    )

    val button3d = Button(context).apply {
        id = R.id.button_3d
        text = context.getString(R.string.view_in_3d)
    }
    root.addView(
        button3d,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            bottomMargin = context.resources.getDimensionPixelSize(R.dimen.default_bottom_margin)
        }
    )

    return AstronomyViews(
        root = root,
        astronomyTitle = title,
        displayDate = displayDate,
        sunMoonChart = chart,
        timeSeekerPanel = timeSeekerPanel,
        seekTime = seekTime,
        closeSeek = closeSeek,
        timeSeeker = timeSeeker,
        sunPositionText = sunPosition,
        moonPositionText = moonPosition,
        astronomyDetailList = list,
        button3d = button3d
    )
}

private fun Int.dp(context: Context): Int {
    return com.kylecorry.andromeda.core.system.Resources.dp(context, toFloat()).toInt()
}

@Preview(showBackground = true)
@Composable
private fun AstronomyHostPreview() {
    TrailSenseTheme {
        AstronomyHost(
            views = createAstronomyViews(LocalContext.current)
        )
    }
}
