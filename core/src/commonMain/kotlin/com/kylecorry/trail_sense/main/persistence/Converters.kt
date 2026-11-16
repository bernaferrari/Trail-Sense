package com.kylecorry.trail_sense.main.persistence

import kotlin.time.Duration.Companion.milliseconds

import androidx.room3.ColumnTypeConverter
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconIcon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.packs.domain.ItemCategory
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.domain.PathPointColoringStyle
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapProjectionType
import kotlin.time.Duration
import kotlin.time.Instant

class Converters {
    @ColumnTypeConverter
    fun fromItemCategory(value: ItemCategory): Int {
        return value.id
    }

    @ColumnTypeConverter
    fun toItemCategory(value: Int): ItemCategory {
        return ItemCategory.values().first { it.id == value }
    }

    @ColumnTypeConverter
    fun fromCloudGenus(value: CloudGenus?): Int? {
        // This was previously the ordinal (zero-indexed), which is why I need to subtract 1
        return value?.id?.toInt()?.minus(1)
    }

    @ColumnTypeConverter
    fun toCloudGenus(value: Int?): CloudGenus? {
        // This was previously the ordinal (zero-indexed), which is why I need to add 1
        return CloudGenus.entries.firstOrNull { it.id == value?.plus(1)?.toLong() }
    }

    @ColumnTypeConverter
    fun fromWeightUnit(value: WeightUnits?): Int? {
        return value?.id
    }

    @ColumnTypeConverter
    fun toWeightUnit(value: Int?): WeightUnits? {
        return WeightUnits.values().firstOrNull { it.id == value }
    }

    @ColumnTypeConverter
    fun fromInstant(value: Instant): Long {
        return value.toEpochMilliseconds()
    }

    @ColumnTypeConverter
    fun toInstant(value: Long): Instant {
        return Instant.fromEpochMilliseconds(value)
    }

    @ColumnTypeConverter
    fun fromDuration(value: Duration): Long {
        return value.inWholeMilliseconds
    }

    @ColumnTypeConverter
    fun toDuration(value: Long): Duration {
        return value.milliseconds
    }

    @ColumnTypeConverter
    fun fromBeaconOwner(value: BeaconOwner): Int {
        return value.id
    }

    @ColumnTypeConverter
    fun toBeaconOwner(value: Int): BeaconOwner {
        return BeaconOwner.values().firstOrNull { it.id == value } ?: BeaconOwner.User
    }

    @ColumnTypeConverter
    fun fromAppColor(value: AppColor): Long {
        return value.id
    }

    @ColumnTypeConverter
    fun toAppColor(value: Long): AppColor {
        return AppColor.values().withId(value) ?: AppColor.Orange
    }

    @ColumnTypeConverter
    fun fromBeaconIcon(value: BeaconIcon?): Long? {
        return value?.id
    }

    @ColumnTypeConverter
    fun toBeaconIcon(value: Long?): BeaconIcon? {
        value ?: return null
        return BeaconIcon.values().withId(value)
    }

    @ColumnTypeConverter
    fun fromPathPointColoringStyle(value: PathPointColoringStyle): Long {
        return value.id
    }

    @ColumnTypeConverter
    fun toPathPointColoringStyle(value: Long): PathPointColoringStyle {
        return PathPointColoringStyle.values().withId(value) ?: PathPointColoringStyle.None
    }

    @ColumnTypeConverter
    fun fromLineStyle(value: LineStyle): Int {
        return value.id
    }

    @ColumnTypeConverter
    fun toLineStyle(value: Int): LineStyle {
        return LineStyle.values().firstOrNull { it.id == value } ?: LineStyle.Dotted
    }

    @ColumnTypeConverter
    fun fromMapProjectionType(mapProjectionType: MapProjectionType): Long {
        return mapProjectionType.id
    }

    @ColumnTypeConverter
    fun toMapProjectionType(value: Long): MapProjectionType {
        return MapProjectionType.values().withId(value) ?: MapProjectionType.Mercator
    }
}
