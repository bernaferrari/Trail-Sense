package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.groups

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.groups.MapGroupEntity

@Dao
interface MapGroupDao {
    @Query("SELECT * FROM map_groups WHERE parent IS :parent")
    suspend fun getAllWithParent(parent: Long?): List<MapGroupEntity>

    @Query("SELECT * FROM map_groups WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): MapGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: MapGroupEntity): Long

    @Delete
    suspend fun delete(group: MapGroupEntity)

    @Update
    suspend fun update(group: MapGroupEntity)
}
