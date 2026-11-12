package com.kylecorry.trail_sense.tools.packs.infrastructure

import kotlinx.coroutines.flow.Flow
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.domain.PackItem

interface IPackRepo {
    suspend fun getItem(id: Long): PackItem?

    suspend fun getItemsFromPackAsync(packId: Long): List<PackItem>

    fun getItemsFromPack(packId: Long): Flow<List<PackItem>>

    fun getPacks(): Flow<List<Pack>>

    suspend fun getPack(packId: Long): Pack?

    suspend fun deleteItem(item: PackItem)

    suspend fun deletePack(pack: Pack)

    suspend fun addPack(pack: Pack): Long

    suspend fun addItem(item: PackItem): Long

    suspend fun deleteAll()

    suspend fun clearPackedAmounts(packId: Long)

    suspend fun copyPack(fromPack: Pack, toPack: Pack): Long
}
