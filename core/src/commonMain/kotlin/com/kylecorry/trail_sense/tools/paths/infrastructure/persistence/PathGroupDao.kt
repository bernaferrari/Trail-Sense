package com.kylecorry.trail_sense.tools.paths.infrastructure.persistence

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update

@Dao
interface PathGroupDao {

    @Query("SELECT * FROM path_groups WHERE parent IS :parent")
    suspend fun getAllWithParent(parent: Long?): List<PathGroupEntity>

    @Query("SELECT * FROM path_groups WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): PathGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: PathGroupEntity): Long

    @Delete
    suspend fun delete(group: PathGroupEntity)

    @Update
    suspend fun update(group: PathGroupEntity)
}
