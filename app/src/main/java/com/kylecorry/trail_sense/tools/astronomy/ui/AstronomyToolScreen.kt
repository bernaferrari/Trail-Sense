@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.kylecorry.trail_sense.tools.astronomy.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.luna.concurrency.onDefault
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
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.quickactions.ComposeQuickActionButtonView
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.gps.MockedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.UserError
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
import com.kylecorry.trail_sense.tools.astronomy.domain.AstroPositions
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyEvent
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.astronomy.domain.MoonTilt
import com.kylecorry.trail_sense.tools.astronomy.quickactions.AstronomyQuickActionBinder
import com.kylecorry.trail_sense.tools.astronomy.ui.commands.AstroChartData
import com.kylecorry.trail_sense.tools.astronomy.ui.commands.CenteredAstroChartDataProvider
import com.kylecorry.trail_sense.tools.astronomy.ui.commands.DailyAstroChartDataProvider
import com.kylecorry.trail_sense.tools.astronomy.ui.commands.IAstroChartDataProvider
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun AstronomyToolScreen(
    quickActionHost: TrailSenseDestinationHost,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val navigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    // The error banner lives on the activity; the screen only reports to it.
    val errorBanner = (LocalActivity.current as? MainActivity)?.errorBanner
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel = viewModel {
        AstronomyViewModel(SensorService(appContext), UserPreferences(appContext))
    }
    val prefs = remember(appContext) { UserPreferences(appContext) }
    val formatService = remember(appContext) { FormatService.getInstance(appContext) }
    val markdownService = remember { DependencyRegistry.get<MarkdownService>() }
    // The rows' text, icons and detail dialogs are presentation, so they are produced here from the ViewModel's
    // date, place and declination.
    val producers = remember(context) {
        listOf(
            SunListItemProducer(context),
            MoonListItemProducer(context),
            MeteorShowerListItemProducer(context),
            LunarEclipseListItemProducer(context),
            SolarEclipseListItemProducer(context)
        )
    }

    // The two quick actions in the app bar; the shared QuickActionFactory still takes the host.
    val leftButton = remember { ComposeQuickActionButtonView() }
    val rightButton = remember { ComposeQuickActionButtonView() }
    DisposableEffect(quickActionHost, lifecycleOwner) {
        AstronomyQuickActionBinder(quickActionHost, lifecycleOwner, leftButton, rightButton, prefs.astronomy).bind()
        onDispose { }
    }

    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        CustomUiUtils.oneTimeToast(
            context,
            context.getString(R.string.tap_sun_moon_hint),
            "cache_tap_sun_moon_shown"
        )
        onPauseOrDispose { viewModel.onPause() }
    }

    LaunchedEffect(viewModel, errorBanner) {
        viewModel.events.collect { event ->
            when (event) {
                AstronomyScreenEvent.LocationNotSet -> errorBanner?.report(
                    UserError(
                        ErrorBannerReason.LocationNotSet,
                        context.getString(R.string.location_not_set),
                        TrailIcons.Satellite,
                        context.getString(R.string.set)
                    ) {
                        errorBanner.dismiss(ErrorBannerReason.LocationNotSet)
                        navigator.navigate(Route.CalibrateGps)
                    }
                )

                AstronomyScreenEvent.NoLocation -> errorBanner?.report(
                    UserError(
                        ErrorBannerReason.NoGPS,
                        context.getString(R.string.location_disabled),
                        TrailIcons.Satellite
                    )
                )
            }
        }
    }

    var astronomyItems by remember { mutableStateOf<List<AstronomyListItem>>(emptyList()) }
    val details = viewModel.details
    LaunchedEffect(details, producers) {
        if (details != null) {
            astronomyItems = onDefault {
                producers.map { it.getListItem(details.date, details.location, details.declination) }
            }.filterNotNull()
        }
    }

    val moonPhase = viewModel.moonPhase
    val moonSize = with(LocalDensity.current) { MoonSize.roundToPx() }
    val moonImage = remember(moonPhase, moonSize) {
        moonPhase?.let {
            MoonPhaseImageMapper(context)
                .getPhaseImage(it.phaseAngle, moonSize, moonSize, it.tilt, plain = true)
                .asImageBitmap()
        }
    }

    val countdown = viewModel.countdown
    val title = when (countdown?.kind) {
        SunCountdownKind.UntilSunrise, SunCountdownKind.UntilSunset ->
            countdown.duration?.let { formatService.formatDuration(it) } ?: ""

        SunCountdownKind.UpNoSet -> stringResource(R.string.sun_up_no_set)
        SunCountdownKind.DownNoRise -> stringResource(R.string.sun_down_no_set)
        null -> ""
    }
    val subtitle = when (countdown?.kind) {
        SunCountdownKind.UntilSunrise -> stringResource(R.string.until_sunrise)
        SunCountdownKind.UntilSunset -> stringResource(R.string.until_sunset)
        SunCountdownKind.UpNoSet -> stringResource(R.string.sun_does_not_set)
        SunCountdownKind.DownNoRise -> stringResource(R.string.sun_does_not_rise)
        null -> ""
    }

    val seeker = if (viewModel.isSeeking) {
        val positions = viewModel.seekPositions
        val sunName = stringResource(R.string.sun)
        val moonName = stringResource(R.string.moon)
        AstronomySeeker(
            time = formatService.formatTime(viewModel.seekTime.toLocalTime(), includeSeconds = false),
            sunPosition = positions?.let {
                positionText(context, markdownService, formatService, sunName, it.sunAltitude, it.sunAzimuth)
            } ?: "",
            moonPosition = positions?.let {
                positionText(context, markdownService, formatService, moonName, it.moonAltitude, it.moonAzimuth)
            } ?: "",
            value = viewModel.seekProgress,
            maxValue = AstronomyViewModel.MAX_SEEK_PROGRESS.toFloat()
        )
    } else {
        null
    }

    val show3d = remember(context) { Tools.isToolAvailable(context, Tools.AUGMENTED_REALITY) } && seeker == null

    val searchOptions = remember {
        listOf(
            AstronomyEvent.FullMoon,
            AstronomyEvent.NewMoon,
            AstronomyEvent.QuarterMoon,
            AstronomyEvent.MeteorShower,
            AstronomyEvent.LunarEclipse,
            AstronomyEvent.SolarEclipse,
            AstronomyEvent.Supermoon
        )
    }

    AstronomyScreen(
        title = title,
        subtitle = subtitle,
        quickActions = listOf(leftButton, rightButton),
        astronomyItems = astronomyItems,
        date = formatService.formatRelativeDate(viewModel.displayDate),
        dateActions = AstronomyDateActions(
            onPrevious = { viewModel.changeDate(viewModel.displayDate.minusDays(1)) },
            onNext = { viewModel.changeDate(viewModel.displayDate.plusDays(1)) },
            onPickDate = {
                CustomUiUtils.pickDate(context, viewModel.displayDate) {
                    if (it != null) {
                        viewModel.changeDate(it)
                    }
                }
            },
            onToday = { viewModel.changeDate(LocalDate.now()) },
            onSearch = {
                val optionNames = listOf(
                    R.string.full_moon,
                    R.string.new_moon,
                    R.string.quarter_moon,
                    R.string.meteor_shower,
                    R.string.lunar_eclipse,
                    R.string.solar_eclipse,
                    R.string.supermoon
                ).map { context.getString(it).capitalizeWords() }
                Pickers.item(
                    context,
                    context.getString(R.string.find_next_occurrence),
                    optionNames,
                    searchOptions.indexOf(viewModel.lastSearch)
                ) {
                    if (it != null) {
                        scope.launch {
                            Alerts.withLoading(context, context.getString(R.string.loading)) {
                                val found = viewModel.findNextEvent(searchOptions[it])
                                if (!found) {
                                    effects.showMessage(
                                        context.getString(
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
        ),
        chart = viewModel.chart,
        moonImage = moonImage,
        onChartClicked = viewModel::startSeeking,
        seeker = seeker,
        onSeek = viewModel::seekTo,
        onCloseSeeker = viewModel::stopSeeking,
        show3d = show3d,
        on3d = {
            openAugmentedReality(
                navigator,
                ARMode.Astronomy,
                enableCamera = prefs.astronomy.startCameraIn3DView,
                time = viewModel.arTime()
            )
        },
        modifier = modifier.fillMaxSize()
    )
}

private fun positionText(
    context: android.content.Context,
    markdownService: MarkdownService,
    formatService: FormatService,
    name: String,
    altitude: Float,
    azimuth: Float
): String {
    return markdownService.toMarkdown(
        context.getString(
            R.string.sun_moon_position_template,
            name,
            formatService.formatDegrees(altitude),
            formatService.formatDegrees(azimuth)
        )
    ).toString()
}

/** The time seeker's text and slider value, shown in place of the bodies while a time is being inspected. */
private data class AstronomySeeker(
    val time: String,
    val sunPosition: String,
    val moonPosition: String,
    val value: Float,
    val maxValue: Float
)

/** What the charted day's header does. */
private class AstronomyDateActions(
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onPickDate: () -> Unit,
    val onToday: () -> Unit,
    val onSearch: () -> Unit
)

internal enum class SunCountdownKind { UntilSunrise, UntilSunset, UpNoSet, DownNoRise }

/** The page's answer: how long until the sun next rises or sets ([duration]), or that it won't today. */
internal data class SunCountdown(val kind: SunCountdownKind, val duration: Duration? = null)

/** The moon's phase and tilt at the time shown, drawn as the chart's moon disc. */
internal data class MoonPhaseSnapshot(val phaseAngle: Float, val tilt: MoonTilt)

/** What the body rows are produced for; [version] changes on every refresh so the rows are produced again. */
internal data class AstronomyDetailsRequest(
    val date: LocalDate,
    val location: Coordinate,
    val declination: Float,
    val version: Int
)

internal sealed interface AstronomyScreenEvent {
    /** The location is mocked but was never set. */
    data object LocationNotSet : AstronomyScreenEvent

    /** Only a cached location is used, and there is none. */
    data object NoLocation : AstronomyScreenEvent
}

/**
 * The astronomy page's state: the charted day, the location, the chart's curves and markers, the sunrise or sunset
 * countdown, the moon's phase and the time seeker. The GPS runs while the page is resumed and the page refreshes
 * every minute.
 */
internal class AstronomyViewModel(
    sensorService: SensorService,
    private val prefs: UserPreferences,
    private val astronomyService: AstronomyService = AstronomyService()
) : ViewModel() {

    private val gps: IGPS = sensorService.getGPS()
    private val declination by lazy { DeclinationFactory().getDeclinationStrategy(prefs, gps) }
    private var sunTimesMode: SunTimesMode = prefs.astronomy.sunTimesMode
    private var chartDataProvider: IAstroChartDataProvider = createChartDataProvider()

    private val eventChannel = Channel<AstronomyScreenEvent>(Channel.BUFFERED)
    val events: Flow<AstronomyScreenEvent> = eventChannel.receiveAsFlow()

    var displayDate by mutableStateOf(LocalDate.now())
        private set
    var location by mutableStateOf(Coordinate.zero)
        private set
    var chart by mutableStateOf(AstroChartState())
        private set
    var moonPhase by mutableStateOf<MoonPhaseSnapshot?>(null)
        private set
    var countdown by mutableStateOf<SunCountdown?>(null)
        private set
    var details by mutableStateOf<AstronomyDetailsRequest?>(null)
        private set
    var isSeeking by mutableStateOf(false)
        private set
    var seekTime by mutableStateOf(ZonedDateTime.now())
        private set
    var seekProgress by mutableFloatStateOf(0f)
        private set
    var seekPositions by mutableStateOf<AstroPositions?>(null)
        private set

    /** The last event searched for, preselected the next time. */
    var lastSearch: AstronomyEvent? = null
        private set

    private var data = AstroChartData(emptyList(), emptyList())
    private var minChartTime = ZonedDateTime.now()
    private var maxChartTime = ZonedDateTime.now()
    private var gpsErrorShown = false
    private var refreshJob: Job? = null
    private var tickerJob: Job? = null
    private var detailsVersion = 0

    init {
        refresh()
    }

    fun onResume() {
        // Settings may have changed while another page was open.
        sunTimesMode = prefs.astronomy.sunTimesMode
        chartDataProvider = createChartDataProvider()
        chart = chart.copy(showBands = prefs.astronomy.showAstronomyBands)
        changeDate(LocalDate.now())
        requestLocationUpdate()
        detectAndShowGPSError()
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(REFRESH_INTERVAL)
                refresh()
            }
        }
        refresh()
    }

    fun onPause() {
        gps.stop(this::onLocationUpdate)
        tickerJob?.cancel()
        tickerJob = null
        gpsErrorShown = false
    }

    override fun onCleared() {
        onPause()
    }

    fun changeDate(date: LocalDate) {
        if (date == displayDate) return
        displayDate = date
        refresh()
    }

    /** Finds the next [event] from the charted day and charts it; false when there is none. */
    suspend fun findNextEvent(event: AstronomyEvent): Boolean {
        lastSearch = event
        val currentDate = displayDate
        val nextEvent = onDefault { astronomyService.findNextEvent(event, location, currentDate) }
        changeDate(nextEvent ?: currentDate)
        refresh()
        return nextEvent != null
    }

    /** The time the 3D view opens at: the inspected time, now, or now's time of day on the charted day. */
    fun arTime(): ZonedDateTime {
        return if (isSeeking) {
            seekTime
        } else if (displayDate == LocalDate.now()) {
            ZonedDateTime.now()
        } else {
            displayDate.atTime(ZonedDateTime.now().toLocalTime()).toZonedDateTime()
        }
    }

    /** Opens the time seeker at [time] (a tap on the chart), or now when the sun or moon was tapped. */
    fun startSeeking(time: ZonedDateTime?) {
        if (isSeeking && time == null) {
            return
        }
        isSeeking = true
        seekTime = time ?: ZonedDateTime.now()
        seekProgress = getSeekProgress().toFloat()
    }

    fun seekTo(value: Float) {
        seekProgress = value
        val progress = value.toInt()
        val seconds = (Duration.between(minChartTime, maxChartTime).seconds * progress /
            MAX_SEEK_PROGRESS.toFloat()).toLong()
        seekTime = minChartTime.plusSeconds(seconds)
        refreshSeekDetails()
    }

    fun stopSeeking() {
        isSeeking = false
        chart = if (displayDate == LocalDate.now()) {
            chart.copy(sunPosition = closest(data.sun), moonPosition = closest(data.moon))
        } else {
            chart.copy(sunPosition = null, moonPosition = null)
        }
    }

    private fun getSeekProgress(): Int {
        val totalDuration = Duration.between(minChartTime, maxChartTime).seconds
        val currentDuration = Duration.between(minChartTime, seekTime).seconds
        val progress = MAX_SEEK_PROGRESS * currentDuration / totalDuration.toFloat()
        return progress.toInt()
    }

    private fun getSunMoonPositions(time: ZonedDateTime): AstroPositions {
        val moonAltitude = astronomyService.getMoonAltitude(location, time)
        val sunAltitude = astronomyService.getSunAltitude(location, time)

        val declination = if (!prefs.compass.useTrueNorth) declination.getDeclination() else 0f

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

    private fun requestLocationUpdate() {
        location = gps.location
        refresh()
        if (gps.hasValidReading) {
            onLocationUpdate()
        } else {
            gps.start(this::onLocationUpdate)
        }
    }

    private fun onLocationUpdate(): Boolean {
        location = gps.location
        refresh()
        return false
    }

    private fun createChartDataProvider(): IAstroChartDataProvider {
        return if (prefs.astronomy.centerSunAndMoon) {
            CenteredAstroChartDataProvider()
        } else {
            DailyAstroChartDataProvider()
        }
    }

    private suspend fun updateMoon() {
        val date = displayDate
        val time = if (isSeeking) {
            seekTime
        } else if (date == LocalDate.now()) {
            ZonedDateTime.now()
        } else {
            date.atTime(12, 0).atZone(ZoneId.systemDefault())
        }

        val phase = onDefault { astronomyService.getMoonPhase(time) }
        val tilt = onDefault { astronomyService.getMoonTilt(location, time) }
        moonPhase = MoonPhaseSnapshot(phase.phaseAngle, tilt)
    }

    private suspend fun updateChart() {
        val date = displayDate
        val place = location
        val provider = chartDataProvider
        data = onDefault {
            val time = if (date == LocalDate.now()) ZonedDateTime.now() else date.atStartOfDay().toZonedDateTime()
            provider.get(place, time)
        }

        minChartTime = data.sun.first().time.toZonedDateTime()
        maxChartTime = minChartTime.atEndOfDay()

        val today = date == LocalDate.now()
        chart = chart.copy(
            startTime = data.sun.firstOrNull()?.time ?: Instant.now(),
            sun = data.sun,
            moon = data.moon,
            sunPosition = if (today) closest(data.sun) else null,
            moonPosition = if (today) closest(data.moon) else null
        )
    }

    private fun closest(altitudes: List<Reading<Float>>, time: ZonedDateTime = ZonedDateTime.now()): Reading<Float>? {
        val instant = time.toInstant()
        return altitudes.minByOrNull { Duration.between(instant, it.time).abs() }
    }

    private suspend fun updateDetails() {
        val date = displayDate
        val place = location
        val declination = onDefault {
            if (!prefs.compass.useTrueNorth) declination.getDeclination() else 0f
        }
        details = AstronomyDetailsRequest(date, place, declination, ++detailsVersion)
    }

    private suspend fun updateCountdown() {
        val currentTime = LocalDateTime.now()
        val place = location
        val mode = sunTimesMode
        val nextSunrise = onDefault { astronomyService.getNextSunrise(place, mode) }
        val nextSunset = onDefault { astronomyService.getNextSunset(place, mode) }

        countdown = if (nextSunrise != null && (nextSunset == null || nextSunrise.isBefore(nextSunset))) {
            SunCountdown(SunCountdownKind.UntilSunrise, Duration.between(currentTime, nextSunrise))
        } else if (nextSunset != null) {
            SunCountdown(SunCountdownKind.UntilSunset, Duration.between(currentTime, nextSunset))
        } else if (astronomyService.isSunUp(place)) {
            SunCountdown(SunCountdownKind.UpNoSet)
        } else {
            SunCountdown(SunCountdownKind.DownNoRise)
        }
    }

    private fun detectAndShowGPSError() {
        if (gpsErrorShown) {
            return
        }

        if (gps is MockedGPS && prefs.gps.locationOverride == Coordinate.zero) {
            eventChannel.trySend(AstronomyScreenEvent.LocationNotSet)
            gpsErrorShown = true
        } else if (gps is CachedGPS && location == Coordinate.zero) {
            eventChannel.trySend(AstronomyScreenEvent.NoLocation)
            gpsErrorShown = true
        }
    }

    private fun refreshSeekDetails() {
        if (!isSeeking) {
            return
        }
        seekPositions = getSunMoonPositions(seekTime)
        if (data.moon.isNotEmpty() && data.sun.isNotEmpty()) {
            val phase = astronomyService.getMoonPhase(seekTime)
            val tilt = astronomyService.getMoonTilt(location, seekTime)
            moonPhase = MoonPhaseSnapshot(phase.phaseAngle, tilt)
            chart = chart.copy(sunPosition = closest(data.sun, seekTime), moonPosition = closest(data.moon, seekTime))
        }
    }

    private fun refresh() {
        detectAndShowGPSError()
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            updateDetails()
            updateChart()
            updateCountdown()
            updateMoon()
            refreshSeekDetails()
        }
    }

    companion object {
        /** The time seeker's steps: one a minute. */
        const val MAX_SEEK_PROGRESS = 60 * 24
        private const val REFRESH_INTERVAL = 60_000L
    }
}

/**
 * The answer on the plain page: the time until the next sunrise or sunset in emphasised type, what it is until
 * underneath, and the two quick actions at its side. No card, no arc: the chart below already shows the sun's place.
 */
@Composable
private fun AstronomyTitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TrailSenseSpacing.lg)
            .testTag("astronomy_title")
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = TrailSenseSpacing.xxs)
            )
        }
    }
}

@Composable
private fun AstronomyDetailList(
    items: List<AstronomyListItem>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.testTag("astronomy_detail_list")) {
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
        color = MaterialTheme.colorScheme.surfaceContainer,
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
                        rotation = icon.rotation,
                        isPhase = icon.bitmap != null
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
    rotation: Float,
    isPhase: Boolean = false
) {
    val shadow = MaterialTheme.colorScheme.surfaceContainerHighest
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        shape = MaterialShapes.Cookie6Sided.toShape(),
        color = primary.copy(alpha = 0.12f),
        contentColor = primary,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isPhase) {
                // A drawn phase (the moon) keeps its shape: a clean disc, the lit part pale and the shadow a quiet
                // tone with a hairline rim, so tonight's phase reads even as a thin crescent.
                Box(
                    Modifier
                        .size(26.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, androidx.compose.foundation.shape.CircleShape)
                ) {
                    androidx.compose.foundation.Image(
                        painter = icon,
                        contentDescription = null,
                        colorFilter = remember(shadow) {
                            androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                                androidx.compose.ui.graphics.ColorMatrix(moonPhaseColorMatrix(shadow, MoonLit))
                            )
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(rotation)
                    )
                }
            } else {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = if (tint == Color.Unspecified) primary else tint,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(rotation)
                )
            }
        }
    }
}

@Composable
private fun AstronomyDataRow(data: List<AstronomyListItemData>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.dp + TrailSenseSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md),
        verticalAlignment = Alignment.Top
    ) {
        data.forEachIndexed { index, it ->
            Column(
                modifier = Modifier
                    .weight(1f)
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
    title: String,
    subtitle: String,
    quickActions: List<ComposeQuickActionButtonView>,
    astronomyItems: List<AstronomyListItem>,
    date: String,
    dateActions: AstronomyDateActions,
    chart: AstroChartState,
    moonImage: ImageBitmap?,
    onChartClicked: (ZonedDateTime?) -> Unit,
    seeker: AstronomySeeker?,
    onSeek: (Float) -> Unit,
    onCloseSeeker: () -> Unit,
    show3d: Boolean,
    on3d: () -> Unit,
    modifier: Modifier = Modifier
) {
    // One question: how long until the light changes? The countdown to sunrise or sunset answers it; the day's chart
    // and the bodies' times sit under it as quiet evidence.
    TrailSensePage(
        title = stringResource(R.string.astronomy),
        modifier = modifier,
        actions = {
            // The user's quick actions, quiet like any app-bar action.
            quickActions
                .filter { it.isVisible }
                .forEach {
                    it.Content(
                        modifier = Modifier.size(48.dp),
                        offContainerColor = Color.Transparent,
                        offContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
        }
    ) {
        item(key = "title") {
            AstronomyTitle(title, subtitle)
        }

        // The sun's rise, noon and set sit right under the countdown, so the answer and its evidence share one
        // glance; the chart and the other bodies follow.
        val sun = astronomyItems.firstOrNull { it.id == SUN_ITEM_ID }
        val others = astronomyItems.filter { it.id != SUN_ITEM_ID }
        if (sun != null && seeker == null) {
            item(key = "sun") {
                AstronomyDetailRow(sun, TrailSenseGroupPosition.Single)
            }
        }

        // The day being charted heads the chart, straight on the page.
        item(key = "day") {
            Column {
                AstronomyDateHeader(
                    date = date,
                    searchEnabled = true,
                    onPrevious = dateActions.onPrevious,
                    onNext = dateActions.onNext,
                    onPickDate = dateActions.onPickDate,
                    onCalendarLongPress = dateActions.onToday,
                    onSearch = dateActions.onSearch
                )
                AstroChart(
                    state = chart,
                    moonImage = moonImage,
                    onChartClicked = onChartClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.chart_height))
                        .testTag("sunMoonChart")
                )
            }
        }

        if (seeker != null) {
            item(key = "seeker") {
                TrailSenseCard(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(TrailSenseShape.groupOuter)
                ) {
                    AstronomyTimeSeekerDetails(seeker, onCloseSeeker)
                    Slider(
                        value = seeker.value,
                        onValueChange = onSeek,
                        valueRange = 0f..seeker.maxValue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = TrailSenseSpacing.md)
                            .testTag("time_seeker")
                    )
                }
            }
        } else {
            item(key = "bodies") {
                AstronomyDetailList(
                    items = others,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (show3d) {
                item(key = "3d") {
                    // A way in, not the point of the page: tonal, with the AR icon. The page has no primary fill.
                    TrailSenseButton(
                        text = stringResource(R.string.view_in_3d),
                        onClick = on3d,
                        icon = TrailIcons.Camera,
                        style = com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle.Tonal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("button_3d")
                    )
                }
            }
        }
    }
}

/** The sun's row id (see SunListItemProducer); it leads the page under the countdown. */
private const val SUN_ITEM_ID = 1L

@Composable
private fun AstronomyTimeSeekerDetails(seeker: AstronomySeeker, onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = seeker.time,
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
                text = seeker.sunPosition,
                modifier = Modifier.weight(1f)
            )
            AstronomyPositionText(
                text = seeker.moonPosition,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AstronomyHostPreview() {
    TrailSenseTheme {
        AstronomyScreen(
            title = "",
            subtitle = "",
            quickActions = emptyList(),
            astronomyItems = emptyList(),
            date = "",
            dateActions = AstronomyDateActions({}, {}, {}, {}, {}),
            chart = AstroChartState(),
            moonImage = null,
            onChartClicked = {},
            seeker = null,
            onSeek = {},
            onCloseSeeker = {},
            show3d = true,
            on3d = {}
        )
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

/**
 * The charted day, like Tides and Climate: the date on the left (tap to pick one, long-press for today), then search
 * when it applies and the previous / next arrows on the right.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AstronomyDateHeader(
    date: String,
    searchEnabled: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPickDate: () -> Unit,
    onCalendarLongPress: () -> Unit,
    onSearch: () -> Unit
) {
    val variant = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = TrailSenseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(TrailSenseShape.groupOuter))
                .combinedClickable(
                    onClickLabel = stringResource(R.string.date),
                    onLongClickLabel = stringResource(R.string.today),
                    onClick = onPickDate,
                    onLongClick = onCalendarLongPress
                )
                .padding(horizontal = TrailSenseSpacing.lg)
                .testTag("date_btn"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                date,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .testTag("date")
            )
            Icon(
                TrailIcons.KeyboardArrowRight,
                contentDescription = null,
                tint = variant,
                modifier = Modifier
                    .padding(start = TrailSenseSpacing.xs)
                    .size(20.dp)
                    .rotate(90f)
            )
        }
        if (searchEnabled) {
            IconButton(
                onClick = onSearch,
                modifier = Modifier.testTag("search_btn")
            ) {
                Icon(TrailIcons.Search, contentDescription = stringResource(R.string.search), tint = variant)
            }
        }
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.testTag("prev_date")
        ) {
            Icon(
                TrailIcons.KeyboardArrowRight,
                contentDescription = stringResource(R.string.previous),
                tint = variant,
                modifier = Modifier.rotate(180f)
            )
        }
        IconButton(
            onClick = onNext,
            modifier = Modifier.testTag("next_date")
        ) {
            Icon(TrailIcons.KeyboardArrowRight, contentDescription = stringResource(R.string.next), tint = variant)
        }
    }
}
