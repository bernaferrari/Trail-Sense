package com.kylecorry.trail_sense.tools.tides.infrastructure.persistence

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.trail_sense.shared.data.Identifiable
import kotlin.time.Instant

@Entity(tableName = "tide_table_rows", indices = [Index(value = ["table_id"])])
data class TideTableRowEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") override val id: Long,
    @ColumnInfo(name = "table_id") val tableId: Long,
    @ColumnInfo(name = "time") val time: Instant,
    @ColumnInfo(name = "high") val isHigh: Boolean,
    @ColumnInfo(name = "height") val heightMeters: Float?
) : Identifiable {

    fun toTide(): Tide {
        return Tide(
            time,
            isHigh,
            heightMeters
        )
    }

    companion object {
        fun from(id: Long, tableId: Long, tide: Tide): TideTableRowEntity {
            return TideTableRowEntity(
                id,
                tableId,
                tide.time,
                tide.isHigh,
                tide.height
            )
        }
    }

}
