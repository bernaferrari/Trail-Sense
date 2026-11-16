package com.kylecorry.trail_sense.plugins.infrastructure.persistence

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "plugin_registrations")
data class PluginRegistrationEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_id")
    val packageId: String,
    @ColumnInfo(name = "version_code")
    val versionCode: Long,
    @ColumnInfo(name = "payload")
    val payload: ByteArray
)
