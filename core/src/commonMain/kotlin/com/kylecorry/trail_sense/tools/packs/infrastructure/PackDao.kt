package com.kylecorry.trail_sense.tools.packs.infrastructure

import kotlinx.coroutines.flow.Flow
import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update

@Dao
interface PackDao {
    @Query("SELECT * FROM packs")
    fun getAll(): Flow<List<PackEntity>>

    @Query("SELECT * FROM packs WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): PackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PackEntity): Long

    @Delete
    suspend fun delete(item: PackEntity)

    @Update
    suspend fun update(item: PackEntity)
}
