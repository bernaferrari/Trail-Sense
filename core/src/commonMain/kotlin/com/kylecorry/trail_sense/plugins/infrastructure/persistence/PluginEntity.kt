package com.kylecorry.trail_sense.plugins.infrastructure.persistence

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "plugins")
data class PluginEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_id")
    val packageId: String,
    @ColumnInfo(name = "signature")
    val signature: String
)
