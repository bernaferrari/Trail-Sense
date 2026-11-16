package com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence

import kotlinx.coroutines.flow.Flow
import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update

@Dao
interface CloudReadingDao {
    @Query("SELECT * FROM clouds")
    fun getAll(): Flow<List<CloudReadingEntity>>

    @Query("SELECT * FROM clouds")
    suspend fun getAllSync(): List<CloudReadingEntity>

    @Query("SELECT * FROM clouds WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): CloudReadingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cloud: CloudReadingEntity): Long

    @Delete
    suspend fun delete(cloud: CloudReadingEntity)

    @Query("DELETE FROM clouds WHERE time < :minEpochMillis")
    suspend fun deleteOlderThan(minEpochMillis: Long)

    @Update
    suspend fun update(cloud: CloudReadingEntity)
}
