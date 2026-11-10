package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.groups

import kotlinx.coroutines.withContext

import kotlinx.coroutines.IO

import kotlinx.coroutines.Dispatchers

import com.kylecorry.trail_sense.shared.grouping.filter.GroupFilter
import com.kylecorry.trail_sense.shared.grouping.persistence.IGroupLoader
import com.kylecorry.trail_sense.shared.grouping.persistence.ISearchableGroupLoader
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap

class MapGroupLoader(private val loader: IGroupLoader<IMap>) : ISearchableGroupLoader<IMap> {

    private val filter = GroupFilter(loader)

    override suspend fun getGroup(id: Long): IMap? {
        return loader.getGroup(id)
    }

    override suspend fun load(search: String?, group: Long?): List<IMap> {
        return if (search.isNullOrBlank()) {
            getMapsByGroup(group)
        } else {
            getMapsBySearch(search, group)
        }
    }

    private suspend fun getMapsBySearch(search: String, groupFilter: Long?) = withContext(Dispatchers.IO) {
        filter.filter(groupFilter) {
            it.name.contains(search, ignoreCase = true)
        }
    }

    private suspend fun getMapsByGroup(group: Long?) = withContext(Dispatchers.IO) {
        loader.getChildren(group, 1)
    }
}
