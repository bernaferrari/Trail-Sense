package com.kylecorry.trail_sense.main.persistence

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Upgrades the original notes-only iOS database to the shared version 62 schema.
 * This is a version 62 snapshot; add a new migration for later schema changes.
 */
internal object NotesToSharedSchema : Migration(1, 62) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `items` (`name` TEXT NOT NULL, `packId` INTEGER NOT NULL, `category` INTEGER NOT NULL, `amount` REAL NOT NULL, `desiredAmount` REAL NOT NULL, `weight` REAL, `weightUnits` INTEGER, `isOptional` INTEGER NOT NULL, `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_items_packId` ON `items` (`packId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `notes` (`title` TEXT, `contents` TEXT, `created` INTEGER NOT NULL, `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `waypoints` (`latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `altitude` REAL, `createdOn` INTEGER NOT NULL, `cellType` INTEGER, `cellQuality` INTEGER, `pathId` INTEGER NOT NULL, `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_waypoints_pathId` ON `waypoints` (`pathId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `pressures` (`pressure` REAL NOT NULL, `altitude` REAL NOT NULL, `altitude_accuracy` REAL, `temperature` REAL NOT NULL, `humidity` REAL NOT NULL, `time` INTEGER NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_pressures_time` ON `pressures` (`time`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `beacons` (`name` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `visible` INTEGER NOT NULL, `comment` TEXT, `beacon_group_id` INTEGER, `elevation` REAL, `temporary` INTEGER NOT NULL, `owner` INTEGER NOT NULL, `color` INTEGER NOT NULL, `icon` INTEGER, `shape` INTEGER NOT NULL DEFAULT 0, `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_beacons_beacon_group_id` ON `beacons` (`beacon_group_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_beacons_temporary_owner` ON `beacons` (`temporary`, `owner`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `beacon_groups` (`name` TEXT NOT NULL, `parent` INTEGER, `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `maps` (`name` TEXT NOT NULL, `filename` TEXT NOT NULL, `latitude1` REAL, `longitude1` REAL, `percentX1` REAL, `percentY1` REAL, `latitude2` REAL, `longitude2` REAL, `percentX2` REAL, `percentY2` REAL, `warped` INTEGER NOT NULL, `rotated` INTEGER NOT NULL, `projection` INTEGER NOT NULL, `rotation` INTEGER NOT NULL, `parent` INTEGER, `pdfWidth` INTEGER, `pdfHeight` INTEGER, `visible` INTEGER NOT NULL, `created_on` INTEGER, `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_maps_parent` ON `maps` (`parent`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `battery` (`percent` REAL NOT NULL, `capacity` REAL NOT NULL, `isCharging` INTEGER NOT NULL, `time` INTEGER NOT NULL, `uptime` INTEGER, `elapsed_realtime` INTEGER, `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_battery_time` ON `battery` (`time`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `packs` (`name` TEXT NOT NULL, `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `clouds` (`time` INTEGER NOT NULL, `genus` INTEGER, `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_clouds_time` ON `clouds` (`time`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `paths` (`name` TEXT, `lineStyle` INTEGER NOT NULL, `pointStyle` INTEGER NOT NULL, `color` INTEGER NOT NULL, `visible` INTEGER NOT NULL, `temporary` INTEGER NOT NULL, `distance` REAL NOT NULL, `numWaypoints` INTEGER NOT NULL, `startTime` INTEGER, `endTime` INTEGER, `north` REAL NOT NULL, `east` REAL NOT NULL, `south` REAL NOT NULL, `west` REAL NOT NULL, `parentId` INTEGER, `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_paths_parentId` ON `paths` (`parentId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `tide_tables` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `latitude` REAL, `longitude` REAL, `isSemidiurnal` INTEGER NOT NULL, `isVisible` INTEGER NOT NULL, `estimateType` INTEGER NOT NULL, `lunitidalInterval` INTEGER, `lunitidalIntervalIsUtc` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `tide_table_rows` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `table_id` INTEGER NOT NULL, `time` INTEGER NOT NULL, `high` INTEGER NOT NULL, `height` REAL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_tide_table_rows_table_id` ON `tide_table_rows` (`table_id`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `path_groups` (`name` TEXT NOT NULL, `parent` INTEGER, `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `lightning` (`time` INTEGER NOT NULL, `distance` REAL NOT NULL, `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `map_groups` (`name` TEXT NOT NULL, `parent` INTEGER, `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `tide_constituents` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `table_id` INTEGER NOT NULL, `constituent_id` INTEGER NOT NULL, `amplitude` REAL NOT NULL, `phase` REAL NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `field_guide_pages` (`name` TEXT NOT NULL, `scientific_name` TEXT, `images` TEXT NOT NULL, `tags` TEXT NOT NULL, `notes` TEXT, `import_id` INTEGER, `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `field_guide_sightings` (`field_guide_page_id` INTEGER NOT NULL, `time` INTEGER, `latitude` REAL, `longitude` REAL, `altitude` REAL, `harvested` INTEGER, `notes` TEXT, `show_on_map` INTEGER NOT NULL, `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_field_guide_sightings_field_guide_page_id` ON `field_guide_sightings` (`field_guide_page_id`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `dem` (`resolution` INTEGER NOT NULL, `compression_method` TEXT NOT NULL, `version` TEXT NOT NULL, `filename` TEXT NOT NULL, `width` INTEGER NOT NULL, `height` INTEGER NOT NULL, `a` REAL NOT NULL, `b` REAL NOT NULL, `north` REAL NOT NULL, `south` REAL NOT NULL, `east` REAL NOT NULL, `west` REAL NOT NULL, `has_water_mask` INTEGER NOT NULL, `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `navigation_bearings` (`bearing` REAL NOT NULL, `start_latitude` REAL, `start_longitude` REAL, `start_time` INTEGER, `is_active` INTEGER NOT NULL, `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `cached_tiles` (`key` TEXT NOT NULL, `x` INTEGER NOT NULL, `y` INTEGER NOT NULL, `z` INTEGER NOT NULL, `filename` TEXT NOT NULL, `created_on` INTEGER NOT NULL, `last_used_on` INTEGER NOT NULL, `size_bytes` INTEGER NOT NULL, `has_alpha` INTEGER NOT NULL, `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_tiles_key_x_y_z` ON `cached_tiles` (`key`, `x`, `y`, `z`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_tiles_last_used_on` ON `cached_tiles` (`last_used_on`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `plugins` (`package_id` TEXT NOT NULL, `signature` TEXT NOT NULL, PRIMARY KEY(`package_id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `plugin_registrations` (`package_id` TEXT NOT NULL, `version_code` INTEGER NOT NULL, `payload` BLOB NOT NULL, PRIMARY KEY(`package_id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `offline_map_files` (`name` TEXT NOT NULL, `type` INTEGER NOT NULL, `path` TEXT NOT NULL, `size_bytes` INTEGER NOT NULL, `created_on` INTEGER NOT NULL, `north` REAL, `east` REAL, `south` REAL, `west` REAL, `attribution` TEXT, `visible` INTEGER NOT NULL, `parent` INTEGER, `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_offline_map_files_parent` ON `offline_map_files` (`parent`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `step_tracking_periods` (`start_time` INTEGER NOT NULL, `end_time` INTEGER, `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_step_tracking_periods_end_time` ON `step_tracking_periods` (`end_time`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `step_count_buckets` (`period_id` INTEGER NOT NULL, `start_time` INTEGER NOT NULL, `end_time` INTEGER NOT NULL, `steps` INTEGER NOT NULL, `active_time` INTEGER NOT NULL, `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_step_count_buckets_period_id` ON `step_count_buckets` (`period_id`)")
    }
}
