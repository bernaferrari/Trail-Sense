package com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconGroup

@Entity(
    tableName = "beacon_groups"
)
data class BeaconGroupEntity(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "parent") val parent: Long? = null
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    fun toBeaconGroup(): BeaconGroup {
        return BeaconGroup(id, name, parent)
    }

    companion object {
        fun from(group: BeaconGroup): BeaconGroupEntity {
            return BeaconGroupEntity(group.name, group.parentId).also {
                it.id = group.id
            }
        }
    }

}
