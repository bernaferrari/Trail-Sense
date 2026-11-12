package com.kylecorry.trail_sense.main

import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.IWeatherRepo
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.WeatherRepo
import com.kylecorry.trail_sense.tools.notes.infrastructure.INoteRepo
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteRepo
import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.ILightningRepo
import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.LightningRepo
import com.kylecorry.trail_sense.tools.battery.infrastructure.persistence.IBatteryRepo
import com.kylecorry.trail_sense.tools.battery.infrastructure.persistence.BatteryRepo
import com.kylecorry.trail_sense.tools.packs.infrastructure.IPackRepo
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.IWaypointRepo
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.WaypointRepo
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.IFlashlightSubsystem
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.IPedometerSubsystem
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.IFieldGuideRepo
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import com.kylecorry.trail_sense.tools.navigation.infrastructure.INavigator
import com.kylecorry.trail_sense.shared.dem.ElevationModel
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem

import android.content.Context
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.TextAlign
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.markdown.MarkdownDelimiter
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.shared.AltitudeCorrection
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.GeoidService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem
import com.kylecorry.trail_sense.shared.device.AndroidDeviceProfile
import com.kylecorry.trail_sense.shared.device.DeviceProfile
import com.kylecorry.trail_sense.shared.device.DeviceSubsystem
import com.kylecorry.trail_sense.shared.getInstance
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.haptics.IHapticSubsystem
import com.kylecorry.trail_sense.shared.sound.TonePlayerFactory
import com.kylecorry.andromeda.sound.AudioTrackSoundPlayer
import com.kylecorry.andromeda.sound.ToneGenerator
import com.kylecorry.trail_sense.shared.images.AndroidBundledImages
import com.kylecorry.trail_sense.shared.images.BundledImages
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.logging.AndroidLogger
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.map_layers.MapLayerLoader
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceRepo
import com.kylecorry.trail_sense.shared.map_layers.tiles.infrastructure.persistance.PersistentTileCache
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.preferences.SessionStateSubsystem
import com.kylecorry.trail_sense.shared.sensors.AndroidSensorService
import com.kylecorry.trail_sense.shared.sensors.ILocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.ISensorSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.text.AndroidAppStrings
import com.kylecorry.trail_sense.shared.text.AndroidRawTextReader
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.RawTextReader
import com.kylecorry.trail_sense.shared.text.StringLoader
import com.kylecorry.trail_sense.shared.text.markdown.MarkdownService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence.MapRepo
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.ITideTableRepo
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideTableRepo
import com.kylecorry.trail_sense.tools.tools.infrastructure.AndroidToolCatalog
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCatalog
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.IWeatherSubsystem
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.AndroidWaterPurificationTimer
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterPurificationTimer
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.AndroidWhiteNoisePlayer
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoisePlayer
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.IPathRepo
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideHarmonicsSource
import com.kylecorry.trail_sense.tools.tides.infrastructure.model.AndroidTideHarmonicsSource
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathRepo

object TrailSenseServiceRegister {
    fun setup(context: Context) {
        val appContext = context.applicationContext

        // Shared services
        DependencyRegistry.addSingleton(appContext)
        DependencyRegistry.addSingleton<Logger>(AndroidLogger(appContext))
        DependencyRegistry.addSingleton(StringLoader(appContext))
        DependencyRegistry.addSingleton<AppStrings>(AndroidAppStrings(appContext))
        DependencyRegistry.addSingleton<RawTextReader>(AndroidRawTextReader(appContext))
        DependencyRegistry.addSingleton<BundledImages>(AndroidBundledImages(appContext))
        DependencyRegistry.addSingleton(FormatService.getInstance(appContext))
        DependencyRegistry.addSingleton(PreferencesSubsystem.getInstance(appContext))
        DependencyRegistry.addSingleton<Preferences>(PreferencesSubsystem.getInstance(appContext).preferences)
        DependencyRegistry.addSingleton(SessionStateSubsystem.getInstance(appContext))
        DependencyRegistry.addSingleton<DeviceProfile>(AndroidDeviceProfile(appContext))
        DependencyRegistry.addSingleton<ToolCatalog>(AndroidToolCatalog(appContext))
        DependencyRegistry.addSingleton(UserPreferences(appContext))
        DependencyRegistry.addSingleton<GeoidService>(AltitudeCorrection)
        DependencyRegistry.addSingleton(NotificationSubsystem(appContext))
        DependencyRegistry.addSingleton(
            MarkdownService(
                delimiters = listOf(
                    // %%text%% is hidden and +text+ centers its paragraph
                    MarkdownDelimiter("%%", MarkdownDelimiter.Effect.Hide),
                    MarkdownDelimiter(
                        "+",
                        MarkdownDelimiter.Effect.Paragraph(ParagraphStyle(textAlign = TextAlign.Center))
                    )
                )
            )
        )
        DependencyRegistry.addSingleton<SensorService>(AndroidSensorService(appContext))
        DependencyRegistry.addSingleton(FileSubsystem.getInstance(appContext))
        DependencyRegistry.addSingleton(AppDatabase.getInstance(appContext))
        DependencyRegistry.addSingleton(SensorSubsystem.getInstance(appContext))
        DependencyRegistry.addSingleton<ISensorSubsystem>(SensorSubsystem.getInstance(appContext))
        DependencyRegistry.addSingleton(LocationSubsystem.getInstance(appContext))
        DependencyRegistry.addSingleton<ILocationSubsystem>(LocationSubsystem.getInstance(appContext))
        DependencyRegistry.addSingleton(DeviceSubsystem(appContext))
        DependencyRegistry.addSingleton(PluginSubsystem.getInstance(appContext))
        DependencyRegistry.addSingleton(HapticSubsystem.getInstance(appContext))
        DependencyRegistry.addSingleton<IHapticSubsystem>(HapticSubsystem.getInstance(appContext))
        DependencyRegistry.addSingleton(TonePlayerFactory::class) {
            TonePlayerFactory { frequency -> AudioTrackSoundPlayer(ToneGenerator().getTone(frequency)) }
        }
        DependencyRegistry.addSingleton(IPathRepo::class) { PathRepo.getInstance(appContext) }
        DependencyRegistry.addSingleton(TideHarmonicsSource::class) { AndroidTideHarmonicsSource(appContext) }
        DependencyRegistry.addSingleton(IPathService::class) { PathService.getInstance() }
        DependencyRegistry.addSingleton<IBeaconService>(BeaconService(appContext))
        DependencyRegistry.addSingleton(IWeatherSubsystem::class) { WeatherSubsystem.getInstance(appContext) }
        DependencyRegistry.addSingleton(IWeatherRepo::class) { WeatherRepo.getInstance(appContext) }
        DependencyRegistry.addSingleton(ITideTableRepo::class) { TideTableRepo.getInstance(appContext) }
        DependencyRegistry.addSingleton(INoteRepo::class) { NoteRepo.getInstance(appContext) }
        DependencyRegistry.addSingleton(ILightningRepo::class) { LightningRepo.getInstance(appContext) }
        DependencyRegistry.addSingleton(IBatteryRepo::class) { BatteryRepo.getInstance(appContext) }
        DependencyRegistry.addSingleton(IPackRepo::class) { PackRepo.getInstance(appContext) }
        DependencyRegistry.addSingleton(IWaypointRepo::class) { WaypointRepo.getInstance(appContext) }
        DependencyRegistry.addSingleton(IFlashlightSubsystem::class) { FlashlightSubsystem.getInstance(appContext) }
        DependencyRegistry.addSingleton(IPedometerSubsystem::class) { PedometerSubsystem.getInstance(appContext) }
        DependencyRegistry.addSingleton(IFieldGuideRepo::class) { FieldGuideRepo.getInstance(appContext) }
        DependencyRegistry.addSingleton(INavigator::class) { Navigator.getInstance(appContext) }
        DependencyRegistry.addSingleton<ElevationModel>(DEM)
        DependencyRegistry.addSingleton<WaterPurificationTimer>(AndroidWaterPurificationTimer(appContext))
        DependencyRegistry.addSingleton<WhiteNoisePlayer>(AndroidWhiteNoisePlayer(appContext))
        DependencyRegistry.addSingleton(MapService::class) { MapService(MapRepo.getInstance(appContext)) }

        // Map layers
        DependencyRegistry.addSingleton(MapLayerLoader(appContext))
        DependencyRegistry.addSingleton(MapLayerPreferenceRepo())
        DependencyRegistry.addSingleton(PersistentTileCache(appContext))

        // Tool registrations on this branch still publish services through Tool.singletons.
        // addSingleton<Any> would register them as Any, so the runtime class is supplied explicitly.
        Tools.getTools(context, false).forEach { tool ->
            tool.singletons.forEach { producer ->
                val service = producer(appContext)
                @Suppress("UNCHECKED_CAST")
                val clazz = service::class as kotlin.reflect.KClass<Any>
                DependencyRegistry.addSingleton(clazz, service)
            }
        }
    }
}
