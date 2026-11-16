package com.kylecorry.trail_sense.web

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.ComposeViewport
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.markdown.MarkdownDelimiter
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.main.persistence.wasmAppDatabase
import com.kylecorry.trail_sense.main.registerSharedRepositories
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.GeoidService
import com.kylecorry.trail_sense.shared.SharedGeoidService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dem.ElevationModel
import com.kylecorry.trail_sense.shared.dem.SharedElevationModel
import com.kylecorry.trail_sense.shared.device.DeviceProfile
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.IEventEmitter
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.haptics.IHapticSubsystem
import com.kylecorry.trail_sense.shared.icons.TrailSenseIcons
import com.kylecorry.trail_sense.shared.images.AppImages
import com.kylecorry.trail_sense.shared.images.BundledImages
import com.kylecorry.trail_sense.shared.images.WebAppImages
import com.kylecorry.trail_sense.shared.images.WebBundledImages
import com.kylecorry.trail_sense.shared.logging.ConsoleLogger
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.SharedNavigationDisplay
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.permissions.BatteryRestrictionPrompt
import com.kylecorry.trail_sense.shared.sensors.ILocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.ISensorSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.SharedLocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SharedSensorService
import com.kylecorry.trail_sense.shared.sensors.SharedSensorSubsystem
import com.kylecorry.trail_sense.shared.sound.TonePlayerFactory
import com.kylecorry.trail_sense.shared.subsystems.SharedFlashlightSubsystem
import com.kylecorry.trail_sense.shared.subsystems.SharedHapticSubsystem
import com.kylecorry.trail_sense.shared.subsystems.SharedPedometerSubsystem
import com.kylecorry.trail_sense.shared.subsystems.SharedTonePlayerFactory
import com.kylecorry.trail_sense.shared.subsystems.SharedWaterPurificationTimer
import com.kylecorry.trail_sense.shared.subsystems.SharedWhiteNoisePlayer
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.ComposeRawTextReader
import com.kylecorry.trail_sense.shared.text.LocalAppStrings
import com.kylecorry.trail_sense.shared.text.RawTextReader
import com.kylecorry.trail_sense.shared.text.WebAppStrings
import com.kylecorry.trail_sense.shared.text.WebPlatformFormats
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.text.markdown.MarkdownService
import com.kylecorry.trail_sense.shared.views.ErrorBannerState
import com.kylecorry.trail_sense.shared.views.LocalErrorBanner
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.ActionSheetModal
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.AppHostDialogs
import com.kylecorry.trail_sense.shared.views.compose.AppHostOverlays
import com.kylecorry.trail_sense.shared.views.compose.AppHostSnackbar
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.SharedComposeDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.SharedDestinationContent
import com.kylecorry.trail_sense.tools.astronomy.ui.AstronomyAlertEnabler
import com.kylecorry.trail_sense.tools.beacons.BeaconBroadcasts
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.SharedBeaconService
import com.kylecorry.trail_sense.tools.climate.infrastructure.SharedClimateRepo
import com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures.ITemperatureRepo
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudRepo
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.BuiltInFieldGuide
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideImageCleanup
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.IFieldGuideRepo
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.IFlashlightSubsystem
import com.kylecorry.trail_sense.tools.navigation.infrastructure.INavigator
import com.kylecorry.trail_sense.tools.navigation.infrastructure.SharedNavigator
import com.kylecorry.trail_sense.tools.navigation.infrastructure.persistence.NavigationBearingService
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMapEntity
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence.IMapRepo
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence.MapFileAccess
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence.MapRepo
import com.kylecorry.trail_sense.tools.paths.PathsBroadcasts
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.infrastructure.BacktrackRestarter
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.IPathRepo
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.IWaypointRepo
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.pedometer.domain.IStepTrackerService
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackerService
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.persistence.StepTrackerRepo
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.IPedometerSubsystem
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideHarmonicsSource
import com.kylecorry.trail_sense.tools.tides.infrastructure.model.SharedTideHarmonicsSource
import com.kylecorry.trail_sense.tools.tools.infrastructure.SharedToolCatalogImpl
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCatalog
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterPurificationTimer
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherMonitorRestarter
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.IWeatherRepo
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.IWeatherSubsystem
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.SharedWeatherSubsystem
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoisePlayer
import kotlinx.browser.document

private var cachedWebStrings: WebAppStrings? = null

private suspend fun ensureInitialized(): WebAppStrings {
    cachedWebStrings?.let { return it }

    val strings = WebAppStrings().apply { preload() }
    val database = wasmAppDatabase()
    val deviceProfile = WebDeviceProfile()
    val rawTexts = ComposeRawTextReader()
    val bundledImages = WebBundledImages()
    val preferences = Preferences()
    val userPreferences = UserPreferences(preferences, deviceProfile)
    val formatter = FormatService(strings, WebPlatformFormats(), userPreferences)

    DependencyRegistry.addSingleton<DeviceProfile>(deviceProfile)
    DependencyRegistry.addSingleton<AppStrings>(strings)
    DependencyRegistry.addSingleton<RawTextReader>(rawTexts)
    DependencyRegistry.addSingleton<BundledImages>(bundledImages)
    DependencyRegistry.addSingleton<AppImages>(WebAppImages())
    DependencyRegistry.addSingleton<Logger>(ConsoleLogger())
    DependencyRegistry.addSingleton(
        MarkdownService(
            listOf(
                MarkdownDelimiter("%%", MarkdownDelimiter.Effect.Hide),
                MarkdownDelimiter("+", MarkdownDelimiter.Effect.Paragraph(ParagraphStyle(textAlign = TextAlign.Center)))
            )
        )
    )
    DependencyRegistry.addSingleton(preferences)
    DependencyRegistry.addSingleton(userPreferences)
    DependencyRegistry.addSingleton(formatter)

    registerSharedRepositories(
        database = database,
        preferences = preferences,
        nearbyName = { strings.get(Res.string.navigation_nearby_category) },
        onPathsChanged = { ToolBroadcasts.broadcast(PathsBroadcasts.PATHS_CHANGED) },
        onBeaconsChanged = { ToolBroadcasts.broadcast(BeaconBroadcasts.BEACONS_CHANGED) }
    )

    DependencyRegistry.addSingleton<ElevationModel>(SharedElevationModel())
    DependencyRegistry.addSingleton<GeoidService>(SharedGeoidService())

    val eventEmitter = object : IEventEmitter {
        override fun broadcast(eventId: String, data: EventData?) = ToolBroadcasts.broadcast(eventId, data)
    }
    val stepRepo = StepTrackerRepo(database.stepTrackerDao())
    val stepService = StepTrackerService(stepRepo, eventEmitter, userPreferences.pedometer)
    DependencyRegistry.addSingleton(stepService)
    DependencyRegistry.addSingleton<IStepTrackerService>(stepService)

    val sensorService = SharedSensorService(preferences, userPreferences, deviceProfile)
    DependencyRegistry.addSingleton<SensorService>(sensorService)
    val sensorSubsystem = SharedSensorSubsystem(sensorService, preferences, userPreferences)
    DependencyRegistry.addSingleton<ISensorSubsystem>(sensorSubsystem)
    val locationSubsystem = SharedLocationSubsystem(sensorService, sensorSubsystem, preferences, userPreferences)
    DependencyRegistry.addSingleton<ILocationSubsystem>(locationSubsystem)

    val pathService = PathService(
        getAppService<IPathRepo>(),
        getAppService<IWaypointRepo>(),
        userPreferences.paths,
        preferences
    )
    DependencyRegistry.addSingleton<IPathService>(pathService)

    val beaconService = SharedBeaconService(getAppService<BeaconRepo>())
    DependencyRegistry.addSingleton<IBeaconService>(beaconService)

    val sharedNavigator = SharedNavigator(
        sessionPrefs = preferences,
        appPrefs = preferences,
        userPrefs = userPreferences,
        beacons = beaconService,
        bearings = getAppService<NavigationBearingService>(),
        locationSubsystem = locationSubsystem,
        pathService = pathService,
        elevationModel = getAppService<ElevationModel>()
    )
    DependencyRegistry.addSingleton<INavigator>(sharedNavigator)

    DependencyRegistry.addSingleton<TideHarmonicsSource>(SharedTideHarmonicsSource(locationSubsystem))
    val climateRepo = SharedClimateRepo()
    DependencyRegistry.addSingleton(climateRepo)
    DependencyRegistry.addSingleton<ITemperatureRepo>(climateRepo)
    DependencyRegistry.addSingleton<IWeatherSubsystem>(
        SharedWeatherSubsystem(
            weatherRepo = getAppService<IWeatherRepo>(),
            cloudRepo = getAppService<CloudRepo>(),
            climate = climateRepo,
            prefs = userPreferences,
            sharedPrefs = preferences,
            location = locationSubsystem,
            sensorService = sensorService
        )
    )

    DependencyRegistry.addSingleton<IHapticSubsystem>(SharedHapticSubsystem())
    DependencyRegistry.addSingleton<IFlashlightSubsystem>(SharedFlashlightSubsystem(preferences, userPreferences))
    DependencyRegistry.addSingleton<IPedometerSubsystem>(
        SharedPedometerSubsystem(preferences, userPreferences, stepService, sensorService)
    )
    DependencyRegistry.addSingleton<TonePlayerFactory>(SharedTonePlayerFactory())
    DependencyRegistry.addSingleton<WhiteNoisePlayer>(SharedWhiteNoisePlayer(preferences))
    DependencyRegistry.addSingleton<WaterPurificationTimer>(SharedWaterPurificationTimer(preferences))
    DependencyRegistry.addSingleton(AstronomyAlertEnabler { serviceId, _, _ ->
        tryOrDefault(Unit) {
            getAppService<ToolCatalog>().getService(serviceId)?.let { /* toggled via ViewModel */ }
        }
    })
    DependencyRegistry.addSingleton(BacktrackRestarter {})
    DependencyRegistry.addSingleton(WeatherMonitorRestarter {})
    DependencyRegistry.addSingleton(BatteryRestrictionPrompt { _ -> })

    val builtInFieldGuide = BuiltInFieldGuide(rawTexts) { "android-assets://$it" }
    DependencyRegistry.addSingleton<IFieldGuideRepo>(
        FieldGuideRepo(
            pageDao = database.fieldGuidePageDao(),
            sightingDao = database.fieldGuideSightingDao(),
            preferences = userPreferences.fieldGuide,
            builtInPages = { builtInFieldGuide.getFieldGuide() },
            builtInPage = { builtInFieldGuide.getFieldGuidePage(it) },
            deleteFile = {}
        )
    )
    DependencyRegistry.addSingleton(FieldGuideImageCleanup {})

    val mapRepo: IMapRepo = MapRepo(
        offlineMapFileDao = database.vectorMapDao(),
        photoMapDao = database.photoMapDao(),
        mapGroupDao = database.mapGroupDao(),
        files = object : MapFileAccess {
            override fun delete(path: String) {}
            override fun canRead(path: String): Boolean = false
            override fun releaseExternal(path: String) {}
            override suspend fun copyToAppStorage(map: VectorMap): VectorMap? = map
            override fun populatePhotoMap(map: PhotoMapEntity): PhotoMap = map.toMap()
        },
        invalidatePhotoMapCache = {}
    )
    DependencyRegistry.addSingleton(MapService(mapRepo))

    DependencyRegistry.addSingleton<ToolCatalog>(
        SharedToolCatalogImpl(
            strings = strings,
            device = deviceProfile,
            prefs = preferences,
            hasMotion = false
        )
    )

    cachedWebStrings = strings
    return strings
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val root = document.body ?: return
    ComposeViewport(root) {
        var loadedStrings by remember { mutableStateOf(cachedWebStrings) }
        LaunchedEffect(Unit) {
            if (loadedStrings == null) {
                loadedStrings = ensureInitialized()
            }
        }

        val activeStrings = loadedStrings ?: return@ComposeViewport

        val navigator = remember { TrailSenseNavigator().apply { setStartRoute(Route.Tools) } }
        val host = remember { AppHost().also(AppHost::attach) }
        val errorBanner = remember { ErrorBannerState() }
        val scope = rememberCoroutineScope()
        val lifecycleOwner = LocalLifecycleOwner.current
        val viewModelOwner = remember {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        }
        DisposableEffect(host, viewModelOwner) {
            onDispose {
                AppHost.detach(host)
                viewModelOwner.viewModelStore.clear()
            }
        }

        val currentRoute = navigator.currentRoute
        val tabRoutes = remember {
            listOf(
                Route.Navigation(),
                Route.Tools,
                Route.Weather,
                Route.Settings
            )
        }
        val selectedTab = when (currentRoute) {
            is Route.Navigation -> 0
            Route.Weather -> 2
            Route.Settings -> 3
            else -> 1
        }
        val showBottomBar = currentRoute in tabRoutes

        CompositionLocalProvider(
            LocalAppStrings provides activeStrings,
            LocalNavigator provides navigator,
            LocalViewModelStoreOwner provides viewModelOwner,
            LocalErrorBanner provides errorBanner
        ) {
            TrailSenseTheme {
                Box(Modifier.fillMaxSize()) {
                    Scaffold(
                        bottomBar = {
                            if (showBottomBar) {
                                NavigationBar {
                                    val labels = listOf(
                                        appString(Res.string.navigation),
                                        appString(Res.string.tools),
                                        appString(Res.string.weather),
                                        appString(Res.string.settings)
                                    )
                                    val icons = listOf(
                                        TrailSenseIcons.Outlined.Explore to TrailSenseIcons.Filled.Explore,
                                        TrailSenseIcons.Outlined.Apps to TrailSenseIcons.Filled.Apps,
                                        TrailSenseIcons.Outlined.PartlyCloudyDay to TrailSenseIcons.Filled.PartlyCloudyDay,
                                        TrailSenseIcons.Filled.Settings to TrailSenseIcons.Filled.Settings
                                    )
                                    tabRoutes.forEachIndexed { index, target ->
                                        val isSelected = selectedTab == index
                                        val (unselectedIcon, selectedIcon) = icons[index]
                                        NavigationBarItem(
                                            selected = isSelected,
                                            onClick = {
                                                if (currentRoute != target) {
                                                    navigator.switchTab(target)
                                                }
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = if (isSelected) selectedIcon else unselectedIcon,
                                                    contentDescription = labels[index]
                                                )
                                            },
                                            label = { Text(labels[index]) }
                                        )
                                    }
                                }
                            }
                        }
                    ) { padding ->
                        SharedNavigationDisplay(navigator, Modifier.padding(padding)) { route ->
                            val destinationHost = remember(route, navigator, lifecycleOwner, host, scope) {
                                SharedComposeDestinationHost(
                                    navigator = navigator,
                                    lifecycleOwner = lifecycleOwner,
                                    route = route,
                                    appHost = host,
                                    scope = scope
                                )
                            }
                            CompositionLocalProvider(LocalScreenEffects provides destinationHost) {
                                destinationHost.SharedDestinationContent(Modifier.fillMaxSize())
                                ActionSheetModal(destinationHost)
                            }
                        }
                    }
                    AppHostOverlays(host)
                    AppHostSnackbar(host)
                    AppHostDialogs(host)
                }
            }
        }
    }
}
