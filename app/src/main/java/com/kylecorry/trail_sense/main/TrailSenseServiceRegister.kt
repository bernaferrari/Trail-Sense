package com.kylecorry.trail_sense.main

import android.content.Context
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.shared.text.markdown.MarkdownService
import com.kylecorry.andromeda.markdown.MarkdownDelimiter
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.TextAlign
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.plugins.PluginSubsystem
import com.kylecorry.trail_sense.shared.AltitudeCorrection
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.getInstance
import com.kylecorry.trail_sense.shared.GeoidService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem
import com.kylecorry.trail_sense.shared.device.AndroidDeviceProfile
import com.kylecorry.trail_sense.shared.device.DeviceProfile
import com.kylecorry.trail_sense.shared.device.DeviceSubsystem
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.logging.AndroidLogger
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.map_layers.MapLayerLoader
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceRepo
import com.kylecorry.trail_sense.shared.map_layers.tiles.infrastructure.persistance.PersistentTileCache
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.preferences.SessionStateSubsystem
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.text.AndroidAppStrings
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.StringLoader
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object TrailSenseServiceRegister {
    fun setup(context: Context) {
        val appContext = context.applicationContext

        // Shared services
        DependencyRegistry.addSingleton(appContext)
        DependencyRegistry.addSingleton<Logger>(AndroidLogger(appContext))
        DependencyRegistry.addSingleton(StringLoader(appContext))
        DependencyRegistry.addSingleton<AppStrings>(AndroidAppStrings(appContext))
        DependencyRegistry.addSingleton(FormatService.getInstance(appContext))
        DependencyRegistry.addSingleton(PreferencesSubsystem.getInstance(appContext))
        DependencyRegistry.addSingleton<Preferences>(PreferencesSubsystem.getInstance(appContext).preferences)
        DependencyRegistry.addSingleton(SessionStateSubsystem.getInstance(appContext))
        DependencyRegistry.addSingleton<DeviceProfile>(AndroidDeviceProfile(appContext))
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
        DependencyRegistry.addSingleton(SensorService(appContext))
        DependencyRegistry.addSingleton(FileSubsystem.getInstance(appContext))
        DependencyRegistry.addSingleton(AppDatabase.getInstance(appContext))
        DependencyRegistry.addSingleton(SensorSubsystem.getInstance(appContext))
        DependencyRegistry.addSingleton(LocationSubsystem.getInstance(appContext))
        DependencyRegistry.addSingleton(DeviceSubsystem(appContext))
        DependencyRegistry.addSingleton(PluginSubsystem.getInstance(appContext))
        DependencyRegistry.addSingleton(HapticSubsystem.getInstance(appContext))

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
