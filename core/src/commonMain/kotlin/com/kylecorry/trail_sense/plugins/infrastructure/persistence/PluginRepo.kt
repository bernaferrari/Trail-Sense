package com.kylecorry.trail_sense.plugins.infrastructure.persistence

import com.kylecorry.luna.concurrency.onIO

class PluginRepo(private val dao: PluginDao) {

    suspend fun getAll(): List<PersistedPlugin> = onIO {
        dao.getAll().map { it.toPlugin() }
    }

    suspend fun get(packageId: String): PersistedPlugin? = onIO {
        dao.get(packageId)?.toPlugin()
    }

    suspend fun upsert(plugin: PersistedPlugin) = onIO {
        dao.upsert(plugin.toEntity())
    }

    suspend fun deleteByPackageId(packageId: String) = onIO {
        dao.deleteByPackageId(packageId)
    }

    private fun PluginEntity.toPlugin(): PersistedPlugin {
        return PersistedPlugin(packageId, signature)
    }

    private fun PersistedPlugin.toEntity(): PluginEntity {
        return PluginEntity(packageId, signature)
    }

}
