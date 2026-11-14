package com.kylecorry.trail_sense.tools.packs.infrastructure

import com.kylecorry.sol.units.Weight
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.domain.PackItem

class PackMapper {

    fun mapToPackItem(item: PackItemEntity): PackItem {
        val value = item.weight
        val units = item.weightUnits
        val weight = if (value != null && units != null) {
            Weight.from(value, units)
        } else {
            null
        }
        return PackItem(
            item.id,
            item.packId,
            item.name,
            item.category,
            item.amount,
            item.desiredAmount,
            weight,
            item.isOptional
        )
    }

    fun mapToItemEntity(item: PackItem): PackItemEntity {
        return PackItemEntity(
            item.name,
            item.packId,
            item.category,
            item.amount,
            item.desiredAmount,
            item.weight?.value,
            item.weight?.units,
            item.isOptional
        ).also {
            it.id = item.id
        }
    }

    fun mapToPack(pack: PackEntity): Pack {
        return Pack(pack.id, pack.name)
    }

    fun mapToPackEntity(pack: Pack): PackEntity {
        return PackEntity(pack.name).also {
            it.id = pack.id
        }
    }

}
