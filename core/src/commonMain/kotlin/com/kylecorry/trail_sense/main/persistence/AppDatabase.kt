package com.kylecorry.trail_sense.main.persistence

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.kylecorry.trail_sense.plugins.infrastructure.persistence.PluginDao
import com.kylecorry.trail_sense.plugins.infrastructure.persistence.PluginEntity
import com.kylecorry.trail_sense.plugins.infrastructure.persistence.PluginRegistrationDao
import com.kylecorry.trail_sense.plugins.infrastructure.persistence.PluginRegistrationEntity
import com.kylecorry.trail_sense.shared.dem.DigitalElevationModelDao
import com.kylecorry.trail_sense.shared.dem.DigitalElevationModelEntity
import com.kylecorry.trail_sense.shared.map_layers.tiles.infrastructure.persistance.CachedTileDao
import com.kylecorry.trail_sense.shared.map_layers.tiles.infrastructure.persistance.CachedTileEntity
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReadingEntity
import com.kylecorry.trail_sense.tools.battery.infrastructure.persistence.BatteryDao
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconDao
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconEntity
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconGroupDao
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconGroupEntity
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudReadingDao
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudReadingEntity
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuidePageDao
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuidePageEntity
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideSightingDao
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideSightingEntity
import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.LightningStrikeDao
import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.LightningStrikeEntity
import com.kylecorry.trail_sense.tools.navigation.infrastructure.persistence.NavigationBearingDao
import com.kylecorry.trail_sense.tools.navigation.infrastructure.persistence.NavigationBearingEntity
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteEntity
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteDao
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMapEntity
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.groups.MapGroupDao
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.groups.MapGroupEntity
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.PhotoMapDao
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.persistence.VectorMapDao
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.persistence.VectorMapEntity
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackDao
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackEntity
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackItemDao
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackItemEntity
import com.kylecorry.trail_sense.tools.paths.domain.WaypointEntity
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathDao
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathEntity
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathGroupDao
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathGroupEntity
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.WaypointDao
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.persistence.StepCountBucketEntity
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.persistence.StepTrackerDao
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.persistence.StepTrackingPeriodEntity
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideConstituentEntry
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideTableDao
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideTableEntity
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideTableRowEntity
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.PressureReadingDao
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.PressureReadingEntity

@Database(
    entities = [PackItemEntity::class, NoteEntity::class, WaypointEntity::class, PressureReadingEntity::class, BeaconEntity::class, BeaconGroupEntity::class, PhotoMapEntity::class, BatteryReadingEntity::class, PackEntity::class, CloudReadingEntity::class, PathEntity::class, TideTableEntity::class, TideTableRowEntity::class, PathGroupEntity::class, LightningStrikeEntity::class, MapGroupEntity::class, TideConstituentEntry::class, FieldGuidePageEntity::class, FieldGuideSightingEntity::class, DigitalElevationModelEntity::class, NavigationBearingEntity::class, CachedTileEntity::class, PluginEntity::class, PluginRegistrationEntity::class, VectorMapEntity::class, StepTrackingPeriodEntity::class, StepCountBucketEntity::class],
    version = 62,
    exportSchema = false
)
@TypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun packItemDao(): PackItemDao
    abstract fun packDao(): PackDao
    abstract fun waypointDao(): WaypointDao
    abstract fun tideTableDao(): TideTableDao
    abstract fun pressureDao(): PressureReadingDao
    abstract fun beaconDao(): BeaconDao
    abstract fun beaconGroupDao(): BeaconGroupDao
    abstract fun noteDao(): NoteDao
    abstract fun photoMapDao(): PhotoMapDao
    abstract fun mapGroupDao(): MapGroupDao
    abstract fun batteryDao(): BatteryDao
    abstract fun cloudDao(): CloudReadingDao
    abstract fun pathDao(): PathDao
    abstract fun pathGroupDao(): PathGroupDao
    abstract fun lightningDao(): LightningStrikeDao
    abstract fun fieldGuidePageDao(): FieldGuidePageDao
    abstract fun fieldGuideSightingDao(): FieldGuideSightingDao
    abstract fun digitalElevationModelDao(): DigitalElevationModelDao
    abstract fun bearingDao(): NavigationBearingDao
    abstract fun cachedTileDao(): CachedTileDao
    abstract fun pluginDao(): PluginDao
    abstract fun pluginRegistrationDao(): PluginRegistrationDao
    abstract fun vectorMapDao(): VectorMapDao
    abstract fun stepTrackerDao(): StepTrackerDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
