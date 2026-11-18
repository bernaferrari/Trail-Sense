package com.kylecorry.trail_sense.main

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.TextAlign
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.markdown.MarkdownDelimiter
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.torch.deviceTorch
import com.kylecorry.andromeda.torch.screenBrightness
import com.kylecorry.trail_sense.main.persistence.AppDatabase
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
import com.kylecorry.trail_sense.shared.logging.ConsoleLogger
import com.kylecorry.trail_sense.shared.logging.Logger
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
import com.kylecorry.trail_sense.shared.text.PlatformFormats
import com.kylecorry.trail_sense.shared.text.RawTextReader
import com.kylecorry.trail_sense.shared.text.markdown.MarkdownService
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

internal fun registerSharedApp(
    database: AppDatabase,
    deviceProfile: DeviceProfile,
    strings: AppStrings,
    bundledImages: BundledImages,
    images: AppImages,
    formats: PlatformFormats,
    hasMotion: Boolean = false,
    hasCamera: Boolean = false
) {
    val rawTexts = ComposeRawTextReader()
    val preferences = Preferences()
    val userPreferences = UserPreferences(preferences, deviceProfile)
    val formatter = FormatService(strings, formats, userPreferences)

    DependencyRegistry.addSingleton<DeviceProfile>(deviceProfile)
    DependencyRegistry.addSingleton<AppStrings>(strings)
    DependencyRegistry.addSingleton<RawTextReader>(rawTexts)
    DependencyRegistry.addSingleton<BundledImages>(bundledImages)
    DependencyRegistry.addSingleton<AppImages>(images)
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
    DependencyRegistry.addSingleton(AstronomyAlertEnabler { _, _, _ -> })
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
    DependencyRegistry.addSingleton<ToolCatalog>(
        SharedToolCatalogImpl(
            strings = strings,
            device = deviceProfile,
            hasMotion = hasMotion,
            hasTorch = deviceTorch().isAvailable(),
            hasScreenLight = screenBrightness() != null,
            hasCamera = hasCamera
        )
    )
}
