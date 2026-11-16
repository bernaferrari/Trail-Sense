package com.kylecorry.trail_sense.plugins.infrastructure.persistence

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert

@Dao
interface PluginRegistrationDao {

    @Query("SELECT * FROM plugin_registrations WHERE package_id = :packageId LIMIT 1")
    suspend fun get(packageId: String): PluginRegistrationEntity?

    @Upsert
    suspend fun upsert(registration: PluginRegistrationEntity)

    @Query("DELETE FROM plugin_registrations WHERE package_id = :packageId")
    suspend fun deleteByPackageId(packageId: String)
}
