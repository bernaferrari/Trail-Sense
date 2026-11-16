package com.kylecorry.trail_sense.tools.weather.infrastructure.persistence

import kotlinx.coroutines.flow.Flow
import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update

@Dao
interface PressureReadingDao {
    @Query("SELECT * FROM pressures")
    fun getAll(): Flow<List<PressureReadingEntity>>

    @Query("SELECT * FROM pressures")
    suspend fun getAllSync(): List<PressureReadingEntity>

    @Query("SELECT * FROM pressures WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): PressureReadingEntity?

    @Query("SELECT * FROM pressures ORDER BY _id DESC LIMIT 1")
    suspend fun getLast(): PressureReadingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pressure: PressureReadingEntity): Long

    @Delete
    suspend fun delete(pressure: PressureReadingEntity)

    @Query("DELETE FROM pressures")
    suspend fun deleteAll()

    @Query("DELETE FROM pressures WHERE time < :minEpochMillis")
    suspend fun deleteOlderThan(minEpochMillis: Long)

    @Update
    suspend fun update(pressure: PressureReadingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun bulkInsert(pressures: List<PressureReadingEntity>)

    @Update
    suspend fun bulkUpdate(pressures: List<PressureReadingEntity>)
}
