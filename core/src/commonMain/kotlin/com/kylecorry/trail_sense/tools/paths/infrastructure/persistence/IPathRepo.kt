package com.kylecorry.trail_sense.tools.paths.infrastructure.persistence

import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathGroup
import kotlinx.coroutines.flow.Flow

/** The saved paths and path groups (without their points). */
interface IPathRepo {
    /** A key that increments whenever a path or group changes. */
    val changeKey: Int

    suspend fun add(value: Path): Long
    suspend fun delete(value: Path)
    suspend fun get(id: Long): Path?
    fun getLive(id: Long): Flow<Path?>
    suspend fun getAll(): List<Path>
    fun getPaths(): Flow<List<Path>>
    suspend fun getPathsWithParent(parent: Long?): List<Path>
    suspend fun addGroup(group: PathGroup): Long
    suspend fun deleteGroup(group: PathGroup)
    suspend fun getGroupsWithParent(parent: Long?): List<PathGroup>
    suspend fun getGroup(id: Long): PathGroup?
}
