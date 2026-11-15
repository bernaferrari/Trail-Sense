package com.kylecorry.trail_sense.main

import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.tools.battery.infrastructure.persistence.BatteryRepo
import com.kylecorry.trail_sense.tools.battery.infrastructure.persistence.IBatteryRepo
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudRepo
import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.ILightningRepo
import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.LightningRepo
import com.kylecorry.trail_sense.tools.navigation.infrastructure.persistence.NavigationBearingService
import com.kylecorry.trail_sense.tools.notes.infrastructure.INoteRepo
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteRepo
import com.kylecorry.trail_sense.tools.packs.infrastructure.IPackRepo
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.IPathRepo
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.IWaypointRepo
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathRepo
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.WaypointRepo
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.ITideTableRepo
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideTableRepo
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.IWeatherRepo
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.WeatherRepo

/** Registers the same persisted repositories on each platform. */
fun registerSharedRepositories(
    database: AppDatabase,
    preferences: Preferences,
    nearbyName: () -> String,
    onPathsChanged: () -> Unit,
    onBeaconsChanged: () -> Unit
) {
    DependencyRegistry.addSingleton(IPathRepo::class) { PathRepo(database.pathDao(), database.pathGroupDao(), onPathsChanged) }
    DependencyRegistry.addSingleton(BeaconRepo::class) { BeaconRepo(database.beaconDao(), database.beaconGroupDao(), onBeaconsChanged) }
    DependencyRegistry.addSingleton(CloudRepo::class) { CloudRepo(database.cloudDao()) }
    DependencyRegistry.addSingleton(IWeatherRepo::class) { WeatherRepo(database.pressureDao()) }
    DependencyRegistry.addSingleton(ITideTableRepo::class) { TideTableRepo(database.tideTableDao(), nearbyName) }
    DependencyRegistry.addSingleton(INoteRepo::class) { NoteRepo(database.noteDao()) }
    DependencyRegistry.addSingleton(ILightningRepo::class) { LightningRepo(database.lightningDao()) }
    DependencyRegistry.addSingleton(IBatteryRepo::class) { BatteryRepo(database.batteryDao(), preferences) }
    DependencyRegistry.addSingleton(IPackRepo::class) { PackRepo(database.packItemDao(), database.packDao()) }
    DependencyRegistry.addSingleton(IWaypointRepo::class) { WaypointRepo(database.waypointDao()) }
    DependencyRegistry.addSingleton(NavigationBearingService::class) { NavigationBearingService(database.bearingDao()) }
}
