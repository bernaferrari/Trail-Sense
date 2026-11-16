package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Query
import androidx.room3.Upsert
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMapEntity

@Dao
interface PhotoMapDao {
    @Query("SELECT * FROM maps")
    suspend fun getAll(): List<PhotoMapEntity>

    @Query("SELECT * FROM maps where parent IS :parent")
    suspend fun getAllWithParent(parent: Long?): List<PhotoMapEntity>

    @Query("SELECT * FROM maps WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): PhotoMapEntity?

    @Upsert
    suspend fun upsert(map: PhotoMapEntity): Long

    @Delete
    suspend fun delete(map: PhotoMapEntity)
}
