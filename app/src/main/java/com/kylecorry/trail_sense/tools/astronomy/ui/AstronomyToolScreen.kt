@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.kylecorry.trail_sense.tools.astronomy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialShapes
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material3.toShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAccents
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseToneColors
import com.kylecorry.trail_sense.shared.views.compose.trailSenseWellColors
import com.kylecorry.trail_sense.tools.weather.ui.SkyFocalShape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.text.capitalizeWords
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.sol.time.Time.atEndOfDay
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.ErrorBannerReason
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.gps.MockedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.quickactions.ComposeQuickActionButtonView
import com.kylecorry.trail_sense.shared.views.DatePickerContent
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.UserError
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
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
import com.kylecorry.trail_sense.tools.augmented_reality.ui.openAugmentedReality
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun AstronomyToolScreen(
    host: TrailSenseDestinationHost,
    modifier: Modifier = Modifier
) {
    val controller = remember(host) { AstronomyController(host) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        controller.onCreate()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            controller.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onDestroy()
        }
    }

    controller.Content(modifier = modifier.fillMaxSize())
}

private class AstronomyController(private val host: TrailSenseDestinationHost) {

    private val views = createAstronomyViews(host.requireContext(), this::showTimeSeeker)
    private val isViewReady: Boolean
        get() = isCreated

    private lateinit var gps: IGPS

    private val chart: AstroChart
        get() = views.sunMoonChart

    private lateinit var sunTimesMode: SunTimesMode

    private val sensorService by lazy { SensorService(host.requireContext()) }
    private val prefs by lazy { UserPreferences(host.requireContext()) }
    private val astronomyService = AstronomyService()
    private val formatService by lazy { FormatService.getInstance(host.requireContext()) }
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
    private var isCreated = false

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
            SunListItemProducer(host.requireContext()),
            MoonListItemProducer(host.requireContext()),
            MeteorShowerListItemProducer(host.requireContext()),
            LunarEclipseListItemProducer(host.requireContext()),
            SolarEclipseListItemProducer(host.requireContext())
        )
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        AstronomyScreen(
            views = views,
            astronomyItems = astronomyItems,
            modifier = modifier
        )
    }

    fun onCreate() {
        if (isCreated) {
            return
        }
        isCreated = true

        AstronomyQuickActionBinder(
            host,
            views.astronomyTitle.leftButton,
            views.astronomyTitle.rightButton,
            prefs.astronomy
        ).bind()

        views.button3d.isVisible =
            Tools.isToolAvailable(host.requireContext(), Tools.AUGMENTED_REALITY)
        views.button3d.onClick = {
            val time = if (isSeeking) {
                currentSeekChartTime
            } else if (views.displayDate.date == LocalDate.now()) {
                ZonedDateTime.now()
            } else {
                views.displayDate.date.atTime(ZonedDateTime.now().toLocalTime()).toZonedDateTime()
            }
            openAugmentedReality(
                host.findNavController(),
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
                host.getString(R.string.full_moon),
                host.getString(R.string.new_moon),
                host.getString(R.string.quarter_moon),
                host.getString(R.string.meteor_shower),
                host.getString(R.string.lunar_eclipse),
                host.getString(R.string.solar_eclipse),
                host.getString(R.string.supermoon)
            ).map { it.capitalizeWords() }

            Pickers.item(
                host.requireContext(),
                host.getString(R.string.find_next_occurrence),
                optionNames,
                options.indexOf(lastAstronomyEventSearch)
            ) {
                if (it != null) {
                    val search = options[it]
                    lastAstronomyEventSearch = search
                    val currentDate = views.displayDate.date
                    host.inBackground {
                        Alerts.withLoading(host.requireContext(), host.getString(R.string.loading)) {
                            val nextEvent = onDefault {
                                astronomyService.findNextEvent(
                                    search, location, currentDate
                                )
                            }
                            onMain {
                                val newDate = nextEvent ?: currentDate
                                displayDate = newDate
                                views.displayDate.updateDate(newDate)
                                refreshAstronomy()

                                if (nextEvent == null) {
                                    host.toast(
                                        host.getString(
                                            R.string.unable_to_find_next_astronomy,
                                            optionNames[it].lowercase()
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        gps = sensorService.getGPS()

        sunTimesMode = prefs.astronomy.sunTimesMode

        views.timeSeeker.onValueChange = { value ->
            val progress = value.toInt()
            val seconds = (Duration.between(
                minChartTime, maxChartTime
            ).seconds * progress / maxProgress.toFloat()).toLong()
            currentSeekChartTime = minChartTime.plusSeconds(seconds)
            refreshSeekDetails()
        }

        refreshAstronomy()
    }

    fun onDestroy() {
        onPause()
        isCreated = false
    }

    private fun showTimeSeeker(time: ZonedDateTime?) {
        if (isSeeking && time == null) {
            return
        }
        isSeeking = true
        views.button3d.isVisible = false
        views.timeSeeker.isVisible = true
        currentSeekChartTime = time ?: ZonedDateTime.now()
        views.timeSeekerDetails.time =
            formatService.formatTime(currentSeekChartTime.toLocalTime(), includeSeconds = false)

        views.timeSeeker.value = getSeekProgress().toFloat()

        views.timeSeekerDetails.onClose = {
            hideTimeSeeker()
        }
    }

    private fun updateSeekPositions() {
        val positions = getSunMoonPositions(currentSeekChartTime)
        views.timeSeekerDetails.sunPosition = markdownService.toMarkdown(
            host.getString(
                R.string.sun_moon_position_template,
                host.getString(R.string.sun),
                formatService.formatDegrees(positions.sunAltitude),
                formatService.formatDegrees(positions.sunAzimuth)
            )
        )

        views.timeSeekerDetails.moonPosition = markdownService.toMarkdown(
            host.getString(
                R.string.sun_moon_position_template,
                host.getString(R.string.moon),
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
        views.timeSeeker.isVisible = false
        views.button3d.isVisible =
            Tools.isToolAvailable(host.requireContext(), Tools.AUGMENTED_REALITY)
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
        val moonAltitude = astronomyService.getMoonAltitude(location, time)
        val sunAltitude = astronomyService.getSunAltitude(location, time)

        val declination = if (!prefs.compass.useTrueNorth) getDeclination() else 0f

        val sunAzimuth =
            astronomyService.getSunAzimuth(location, time).withDeclination(-declination).value
        val moonAzimuth =
            astronomyService.getMoonAzimuth(location, time).withDeclination(-declination).value

        return AstroPositions(
            moonAltitude,
            sunAltitude,
            moonAzimuth,
            sunAzimuth
        )
    }

    fun onResume() {
        if (!isCreated) {
            return
        }
        chart.setBands(prefs.astronomy.showAstronomyBands)
        views.displayDate.reset()
        requestLocationUpdate()
        views.displayDate.setDayViewDecorator(AstronomyDayViewDecorator(location))
        detectAndShowGPSError()
        periodicUpdater.interval(60000)
        refreshAstronomy()

        CustomUiUtils.oneTimeToast(
            host.requireContext(),
            host.getString(R.string.tap_sun_moon_hint),
            "cache_tap_sun_moon_shown"
        )
    }

    fun onPause() {
        if (this::gps.isInitialized) {
            gps.stop(this::onLocationUpdate)
        }
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
        val time = if (isSeeking) {
            currentSeekChartTime
        } else if (displayDate == LocalDate.now()) {
            ZonedDateTime.now()
        } else {
            displayDate.atTime(12, 0).atZone(ZoneId.systemDefault())
        }

        val phase = onDefault { astronomyService.getMoonPhase(time) }
        val tilt = onDefault { astronomyService.getMoonTilt(location, time) }

        withContext(Dispatchers.Main) {
            if (!isViewReady) {
                return@withContext
            }
            val size = Resources.dp(host.requireContext(), 24f).toInt()
            chart.setMoonImage(
                MoonPhaseImageMapper(host.requireContext()).getPhaseImage(
                    phase.phaseAngle,
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
        maxChartTime = minChartTime.atEndOfDay()

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
        time: ZonedDateTime = ZonedDateTime.now()
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

        // Where the sun is on today's arc (day) or tonight's (night), for the hero's sun path.
        val rise0 = nextSunrise
        val set0 = nextSunset
        val sunUp = set0 != null && (rise0 == null || set0.isBefore(rise0))
        val arcProgress = run {
            val rise = rise0
            val set = set0
            if (rise == null || set == null) return@run null
            val dayLength = astronomyService.getLengthOfDay(location, sunTimesMode, LocalDate.now()).seconds.toFloat()
            if (dayLength <= 0f) return@run null
            if (sunUp) {
                1f - Duration.between(currentTime, set).seconds / dayLength
            } else {
                1f - Duration.between(currentTime, rise).seconds / (86_400f - dayLength).coerceAtLeast(1f)
            }
        }?.coerceIn(0f, 1f)

        withContext(Dispatchers.Main) {
            views.astronomyTitle.sunUp = sunUp || (nextSunset == null && nextSunrise == null && astronomyService.isSunUp(location))
            views.astronomyTitle.progress = arcProgress
            if (nextSunrise != null && (nextSunset == null || nextSunrise.isBefore(nextSunset))) {
                views.astronomyTitle.title =
                    formatService.formatDuration(Duration.between(currentTime, nextSunrise))
                views.astronomyTitle.subtitle = host.getString(R.string.until_sunrise)
            } else if (nextSunset != null) {
                views.astronomyTitle.title =
                    formatService.formatDuration(Duration.between(currentTime, nextSunset))
                views.astronomyTitle.subtitle = host.getString(R.string.until_sunset)
            } else if (astronomyService.isSunUp(location)) {
                views.astronomyTitle.title = host.getString(R.string.sun_up_no_set)
                views.astronomyTitle.subtitle = host.getString(R.string.sun_does_not_set)
            } else {
                views.astronomyTitle.title = host.getString(R.string.sun_down_no_set)
                views.astronomyTitle.subtitle = host.getString(R.string.sun_does_not_rise)
            }
        }
    }

    private fun detectAndShowGPSError() {
        if (gpsErrorShown) {
            return
        }

        if (gps is MockedGPS && prefs.gps.locationOverride == Coordinate.zero) {
            val activity = host.mainActivity
            val navController = host.findNavController()
            val error = UserError(
                ErrorBannerReason.LocationNotSet,
                host.getString(R.string.location_not_set),
                TrailIcons.Satellite,
                host.getString(R.string.set)
            ) {
                activity.errorBanner.dismiss(ErrorBannerReason.LocationNotSet)
                navController.navigate(R.id.calibrateGPSFragment)
            }
            activity.errorBanner.report(error)
            gpsErrorShown = true
        } else if (gps is CachedGPS && location == Coordinate.zero) {
            val error = UserError(
                ErrorBannerReason.NoGPS, host.getString(R.string.location_disabled), TrailIcons.Satellite
            )
            (host.mainActivity).errorBanner.report(error)
            gpsErrorShown = true
        }
    }

    private fun refreshSeekDetails() {
        if (!isSeeking || !isViewReady) {
            return
        }
        updateSeekPositions()
        views.timeSeekerDetails.time = formatService.formatTime(
            currentSeekChartTime.toLocalTime(),
            includeSeconds = false
        )
        if (data.moon.isNotEmpty() && data.sun.isNotEmpty()) {
            val phase = astronomyService.getMoonPhase(currentSeekChartTime)
            val tilt = astronomyService.getMoonTilt(location, currentSeekChartTime)
            val size = Resources.dp(host.requireContext(), 24f).toInt()
            chart.setMoonImage(
                MoonPhaseImageMapper(host.requireContext()).getPhaseImage(
                    phase.phaseAngle,
                    size,
                    size,
                    tilt
                )
            )
            plotMoonImage(data.moon, currentSeekChartTime)
            plotSunImage(data.sun, currentSeekChartTime)
        }
    }

    private fun refreshAstronomy() {
        if (!isViewReady || !this::gps.isInitialized) {
            return
        }
        detectAndShowGPSError()
        views.displayDate.setDayViewDecorator(AstronomyDayViewDecorator(location))
        host.inBackground {
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
    val astronomyTitle: AstronomyTitleState,
    val displayDate: AstronomyDatePickerState,
    val sunMoonChart: AstroChart,
    val timeSeekerDetails: AstronomyTimeSeekerDetailsState,
    val timeSeeker: AstronomyTimeSeekerState,
    val button3d: AstronomyTextButtonState
)

private class AstronomyTitleState {
    val leftButton = ComposeQuickActionButtonView()
    val rightButton = ComposeQuickActionButtonView()
    var title by mutableStateOf("")
    var subtitle by mutableStateOf("")
    var sunUp by mutableStateOf(true)
    var progress by mutableStateOf<Float?>(null)
}

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

/**
 * The hero: the time until the next sunrise or sunset under a dashed sky arc, with the sun (or the moon at night) as
 * a turning shaped badge at its place on the arc. Warm tertiary by day, cool primary by night. The two quick actions
 * sit in the top corners.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AstronomyTitle(
    state: AstronomyTitleState,
    modifier: Modifier = Modifier
) {
    val tone = if (state.sunUp) TrailSenseTone.Tertiary else TrailSenseTone.Primary
    val colors = trailSenseToneColors(tone)
    val container by androidx.compose.animation.animateColorAsState(
        colors.container,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "astroHero"
    )
    val progress by androidx.compose.animation.core.animateFloatAsState(
        state.progress ?: 0.5f,
        MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "sunArc"
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TrailSenseShape.hero),
        color = container,
        contentColor = colors.content
    ) {
        Box(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(TrailSenseSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                state.leftButton.Content()
                state.rightButton.Content()
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = TrailSenseSpacing.xl, bottom = TrailSenseSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val badgeSize = 56.dp
                BoxWithConstraints(
                    Modifier
                        .size(width = 240.dp, height = 150.dp)
                ) {
                    val track = colors.content.copy(alpha = 0.28f)
                    val trail = colors.accent
                    androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                        val inset = badgeSize.toPx() / 2f
                        val arcRect = androidx.compose.ui.geometry.Rect(
                            inset,
                            inset,
                            size.width - inset,
                            size.width - inset
                        )
                        val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(3.dp.toPx(), 7.dp.toPx())
                        )
                        drawArc(
                            color = track,
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = arcRect.topLeft,
                            size = arcRect.size,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 3.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                pathEffect = dash
                            )
                        )
                        if (state.progress != null) {
                            drawArc(
                                color = trail,
                                startAngle = 180f,
                                sweepAngle = 180f * progress,
                                useCenter = false,
                                topLeft = arcRect.topLeft,
                                size = arcRect.size,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 5.dp.toPx(),
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            )
                        }
                        // The horizon.
                        drawLine(
                            track,
                            androidx.compose.ui.geometry.Offset(0f, arcRect.center.y),
                            androidx.compose.ui.geometry.Offset(size.width, arcRect.center.y),
                            strokeWidth = 2.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                    // The sun or moon at its place on the arc.
                    val radius = (maxWidth - badgeSize) / 2
                    val angle = Math.toRadians((180.0 + 180.0 * progress))
                    val cx = maxWidth / 2 + radius * kotlin.math.cos(angle).toFloat()
                    val cy = badgeSize / 2 + radius + radius * kotlin.math.sin(angle).toFloat()
                    SkyFocalShape(
                        icon = if (state.sunUp) androidx.compose.material.icons.Icons.Rounded.WbSunny else androidx.compose.material.icons.Icons.Rounded.Bedtime,
                        shape = if (state.sunUp) MaterialShapes.Sunny.toShape() else MaterialShapes.Cookie9Sided.toShape(),
                        size = badgeSize,
                        containerColor = colors.accent,
                        contentColor = colors.onAccent,
                        modifier = Modifier.offset(x = cx - badgeSize / 2, y = cy - badgeSize / 2)
                    )
                }
                Spacer(Modifier.height(TrailSenseSpacing.md))
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFeatureSettings = "tnum"
                    ),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                )
                if (state.subtitle.isNotEmpty()) {
                    Text(
                        text = state.subtitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.content.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun AstronomyDetailList(
    items: List<AstronomyListItem>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.testTag("astronomy_detail_list")) {
        TrailSenseGroupHeader(stringResource(R.string.astronomy))
        Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
            items.forEachIndexed { index, item ->
                AstronomyDetailRow(item, TrailSenseGroupPosition.at(index, items.size))
            }
        }
    }
}

/**
 * One body (the sun, the moon, a meteor shower…): its icon, name and state, then its times as an even strip across
 * the row, so a long phase name never squeezes the times and the times always line up from row to row.
 */
@Composable
private fun AstronomyDetailRow(item: AstronomyListItem, position: TrailSenseGroupPosition) {
    Surface(
        shape = trailSenseGroupShape(position),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(item.onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier)
                .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.lg)
                .testTag("astronomy_detail_title"),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                item.icon?.let { icon ->
                    AstronomyIconChip(
                        icon = rememberAstronomyIconPainter(icon),
                        tint = icon.tint?.let { Color(it) } ?: Color.Unspecified,
                        rotation = icon.rotation
                    )
                    Spacer(Modifier.size(TrailSenseSpacing.md))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    item.subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (item.data.isNotEmpty()) AstronomyDataRow(item.data)
        }
    }
}

@Composable
private fun rememberAstronomyIconPainter(icon: com.kylecorry.trail_sense.tools.astronomy.ui.items.AstronomyListIcon): Painter {
    val bitmap = icon.bitmap
    return if (bitmap != null) {
        remember(bitmap) { BitmapPainter(bitmap.asImageBitmap()) }
    } else {
        rememberVectorPainter(requireNotNull(icon.vector))
    }
}

@Composable
private fun AstronomyIconChip(
    icon: Painter,
    tint: Color,
    rotation: Float
) {
    val wells = trailSenseWellColors(if (tint == Color.Unspecified) TrailSenseAccents.Periwinkle else tint)
    Surface(
        shape = MaterialShapes.Cookie9Sided.toShape(),
        color = wells.container,
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(28.dp)
                    .rotate(rotation)
            )
        }
    }
}

@Composable
private fun AstronomyDataRow(data: List<AstronomyListItemData>) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs),
        verticalAlignment = Alignment.Top
    ) {
        // Rise, peak and set read as one strip: warm for the rise, neutral for the peak, cool for the set.
        val tints = if (data.size == 3) {
            listOf(
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            List(data.size) { MaterialTheme.colorScheme.surfaceContainer }
        }
        data.forEachIndexed { index, it ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        tints[index],
                        RoundedCornerShape(
                            topStart = if (index == 0) 18.dp else 6.dp,
                            bottomStart = if (index == 0) 18.dp else 6.dp,
                            topEnd = if (index == data.lastIndex) 18.dp else 6.dp,
                            bottomEnd = if (index == data.lastIndex) 18.dp else 6.dp
                        )
                    )
                    .padding(horizontal = TrailSenseSpacing.md, vertical = TrailSenseSpacing.sm)
                    .testTag("astronomy_detail_data")
            ) {
                Text(
                    text = it.value,
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                it.label?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun AstronomyScreen(
    views: AstronomyViews,
    astronomyItems: List<AstronomyListItem>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(TrailSenseSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
        ) {
            AstronomyTitle(views.astronomyTitle)

            views.displayDate.Content()

            TrailSenseGroupCard(TrailSenseGroupPosition.Single, contentPadding = PaddingValues(TrailSenseSpacing.sm)) {
                views.sunMoonChart.Content(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.chart_height))
                        .testTag("sunMoonChart")
                )
            }

            if (views.timeSeeker.isVisible) {
                TrailSenseCard(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(TrailSenseShape.groupOuter)
                ) {
                    views.timeSeekerDetails.Content()
                    Slider(
                        value = views.timeSeeker.value,
                        onValueChange = views.timeSeeker::updateValue,
                        valueRange = 0f..views.timeSeeker.maxValue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = TrailSenseSpacing.md)
                            .testTag("time_seeker")
                    )
                }
            } else {
                AstronomyDetailList(
                    items = astronomyItems,
                    modifier = Modifier.fillMaxWidth()
                )
                if (views.button3d.isVisible) {
                    views.button3d.Content()
                }
            }
        }
    }
}

private fun createAstronomyViews(
    context: Context,
    onChartClicked: (time: ZonedDateTime?) -> Unit = {}
): AstronomyViews {
    return AstronomyViews(
        astronomyTitle = AstronomyTitleState(),
        displayDate = AstronomyDatePickerState(context),
        sunMoonChart = AstroChart(context, onChartClicked),
        timeSeekerDetails = AstronomyTimeSeekerDetailsState(),
        timeSeeker = AstronomyTimeSeekerState(maxValue = (60 * 24).toFloat()),
        button3d = AstronomyTextButtonState(context.getString(R.string.view_in_3d))
    )
}

private class AstronomyTimeSeekerDetailsState {
    var time by mutableStateOf("")
    var sunPosition by mutableStateOf<CharSequence>("")
    var moonPosition by mutableStateOf<CharSequence>("")
    var onClose by mutableStateOf({})

    @Composable
    fun Content() {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = time,
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(TrailSenseSize.minimumTouchTarget)
                ) {
                    Icon(
                        imageVector = TrailIcons.Cancel,
                        contentDescription = stringResource(android.R.string.cancel),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TrailSenseSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
                AstronomyPositionText(
                    text = sunPosition.toString(),
                    modifier = Modifier.weight(1f)
                )
                AstronomyPositionText(
                    text = moonPosition.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private class AstronomyTimeSeekerState(
    val maxValue: Float
) {
    var isVisible by mutableStateOf(false)
    var value by mutableStateOf(0f)
    var onValueChange by mutableStateOf<(Float) -> Unit>({})

    fun updateValue(newValue: Float) {
        value = newValue
        onValueChange(newValue)
    }
}

@Composable
private fun AstronomyPositionText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontFeatureSettings = "tnum"
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private class AstronomyTextButtonState(
    private val text: String
) {
    var isVisible by mutableStateOf(false)
    var onClick by mutableStateOf({})

    @Composable
    fun Content() {
        // A way in, not the point of the page: tonal, with the AR icon.
        androidx.compose.material3.FilledTonalButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(
                imageVector = TrailIcons.Camera,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(TrailSenseSpacing.sm))
            Text(text, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AstronomyHostPreview() {
    TrailSenseTheme {
        AstronomyScreen(
            views = createAstronomyViews(LocalContext.current),
            astronomyItems = emptyList()
        )
    }
}
