package com.kylecorry.trail_sense.ios

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.markdown.MarkdownDelimiter
import com.mohamedrejeb.calf.sf.symbols.SFSymbol
import com.mohamedrejeb.calf.ui.ExperimentalCalfUiApi
import com.mohamedrejeb.calf.ui.navigation.AdaptiveNavigationBar
import com.mohamedrejeb.calf.ui.navigation.AdaptiveScaffold
import com.mohamedrejeb.calf.ui.navigation.UIKitTabBarConfiguration
import com.mohamedrejeb.calf.ui.navigation.UIKitUITabBarItem
import com.mohamedrejeb.calf.ui.uikit.UIKitImage
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.main.persistence.iosAppDatabase
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
import com.kylecorry.trail_sense.shared.images.AppImages
import com.kylecorry.trail_sense.shared.images.BundledImages
import com.kylecorry.trail_sense.shared.images.IosAppImages
import com.kylecorry.trail_sense.shared.images.IosBundledImages
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
import com.kylecorry.trail_sense.shared.text.ComposeAppStrings
import com.kylecorry.trail_sense.shared.text.ComposeRawTextReader
import com.kylecorry.trail_sense.shared.text.IosPlatformFormats
import com.kylecorry.trail_sense.shared.text.LocalAppStrings
import com.kylecorry.trail_sense.shared.text.RawTextReader
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
import platform.CoreMotion.CMMotionManager
import platform.UIKit.UIViewController

/** Entry point for the iOS host. [databasePath] is in the app's documents directory. */
@OptIn(ExperimentalCalfUiApi::class)
fun MainViewController(databasePath: String): UIViewController {
    val database = iosAppDatabase(databasePath)
    val deviceProfile = IosDeviceProfile()
    val strings = ComposeAppStrings()
    val rawTexts = ComposeRawTextReader()
    val bundledImages = IosBundledImages()
    val preferences = Preferences()
    val userPreferences = UserPreferences(preferences, deviceProfile)
    val formatter = FormatService(strings, IosPlatformFormats(), userPreferences)

    DependencyRegistry.addSingleton<DeviceProfile>(deviceProfile)
    DependencyRegistry.addSingleton<AppStrings>(strings)
    DependencyRegistry.addSingleton<RawTextReader>(rawTexts)
    DependencyRegistry.addSingleton<BundledImages>(bundledImages)
    DependencyRegistry.addSingleton<AppImages>(IosAppImages())
    DependencyRegistry.addSingleton<Logger>(ConsoleLogger())
    DependencyRegistry.addSingleton(MarkdownService(listOf(
        MarkdownDelimiter("%%", MarkdownDelimiter.Effect.Hide),
        MarkdownDelimiter("+", MarkdownDelimiter.Effect.Paragraph(ParagraphStyle(textAlign = TextAlign.Center)))
    )))
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

    // Elevation & geoid models
    DependencyRegistry.addSingleton<ElevationModel>(SharedElevationModel())
    DependencyRegistry.addSingleton<GeoidService>(SharedGeoidService())

    // Step tracker (register under both concrete and interface types)
    val eventEmitter = object : IEventEmitter {
        override fun broadcast(eventId: String, data: EventData?) = ToolBroadcasts.broadcast(eventId, data)
    }
    val stepRepo = StepTrackerRepo(database.stepTrackerDao())
    val stepService = StepTrackerService(stepRepo, eventEmitter, userPreferences.pedometer)
    DependencyRegistry.addSingleton(stepService)
    DependencyRegistry.addSingleton<IStepTrackerService>(stepService)

    // Sensors & location
    val sensorService = SharedSensorService(preferences, userPreferences, deviceProfile)
    DependencyRegistry.addSingleton<SensorService>(sensorService)
    val sensorSubsystem = SharedSensorSubsystem(sensorService, preferences, userPreferences)
    DependencyRegistry.addSingleton<ISensorSubsystem>(sensorSubsystem)
    val locationSubsystem = SharedLocationSubsystem(sensorService, sensorSubsystem, preferences, userPreferences)
    DependencyRegistry.addSingleton<ILocationSubsystem>(locationSubsystem)

    // Path, beacon, and navigation services
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

    // Tides, climate, and weather
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

    // Peripheral subsystems & platform hooks
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

    // Field guide & offline maps repositories
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

    // Tool catalog (registered after sensor dependencies are ready)
    val hasMotion = CMMotionManager().deviceMotionAvailable
    DependencyRegistry.addSingleton<ToolCatalog>(
        SharedToolCatalogImpl(
            strings = strings,
            device = deviceProfile,
            prefs = preferences,
            hasMotion = hasMotion
        )
    )

    return ComposeUIViewController {
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
            LocalAppStrings provides strings,
            LocalNavigator provides navigator,
            LocalViewModelStoreOwner provides viewModelOwner,
            LocalErrorBanner provides errorBanner
        ) {
            TrailSenseTheme {
                Box(Modifier.fillMaxSize()) {
                    AdaptiveScaffold(bottomBar = {
                        if (showBottomBar) {
                            AdaptiveNavigationBar(
                                iosItems = listOf(
                                    UIKitUITabBarItem(
                                        title = appString(Res.string.navigation),
                                        image = UIKitImage.SystemName(SFSymbol.location),
                                        selectedImage = UIKitImage.SystemName(SFSymbol.locationFill)
                                    ),
                                    UIKitUITabBarItem(
                                        title = appString(Res.string.tools),
                                        image = UIKitImage.SystemName(SFSymbol.wrenchAndScrewdriver),
                                        selectedImage = UIKitImage.SystemName(SFSymbol.wrenchAndScrewdriverFill)
                                    ),
                                    UIKitUITabBarItem(
                                        title = appString(Res.string.weather),
                                        image = UIKitImage.SystemName(SFSymbol.cloudSun),
                                        selectedImage = UIKitImage.SystemName(SFSymbol.cloudSunFill)
                                    ),
                                    UIKitUITabBarItem(
                                        title = appString(Res.string.settings),
                                        image = UIKitImage.SystemName(SFSymbol.gearshape),
                                        selectedImage = UIKitImage.SystemName(SFSymbol.gearshapeFill)
                                    )
                                ),
                                iosSelectedIndex = selectedTab,
                                iosOnItemSelected = { index ->
                                    val target = tabRoutes[index]
                                    if (currentRoute != target) {
                                        navigator.switchTab(target)
                                    }
                                },
                                iosConfiguration = UIKitTabBarConfiguration(
                                    selectedItemColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }) { padding ->
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
