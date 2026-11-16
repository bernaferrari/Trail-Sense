package com.kylecorry.trail_sense.tools.packs.infrastructure

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.tools.packs.domain.ItemCategory

@Entity(
    tableName = "items",
    indices = [Index(value = ["packId"])]
)
data class PackItemEntity(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "packId") val packId: Long,
    @ColumnInfo(name = "category") val category: ItemCategory,
    @ColumnInfo(name = "amount") val amount: Double = 0.0,
    @ColumnInfo(name = "desiredAmount") val desiredAmount: Double = 0.0,
    @ColumnInfo(name = "weight") val weight: Float? = null,
    @ColumnInfo(name = "weightUnits") val weightUnits: WeightUnits? = null,
    @ColumnInfo(name = "isOptional") val isOptional: Boolean = false
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0
}
