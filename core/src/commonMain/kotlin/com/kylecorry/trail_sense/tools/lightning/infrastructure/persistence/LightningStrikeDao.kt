package com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence

import kotlinx.coroutines.flow.Flow
import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update

@Dao
interface LightningStrikeDao {
    @Query("SELECT * FROM lightning")
    fun getAll(): Flow<List<LightningStrikeEntity>>

    @Query("SELECT * FROM lightning")
    suspend fun getAllSync(): List<LightningStrikeEntity>

    @Query("SELECT * FROM lightning WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): LightningStrikeEntity?

    @Query("SELECT * FROM lightning ORDER BY _id DESC LIMIT 1")
    suspend fun getLast(): LightningStrikeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(strike: LightningStrikeEntity): Long

    @Delete
    suspend fun delete(strike: LightningStrikeEntity)

    @Query("DELETE FROM lightning WHERE time < :minEpochMillis")
    suspend fun deleteOlderThan(minEpochMillis: Long)

    @Update
    suspend fun update(strike: LightningStrikeEntity)
}
