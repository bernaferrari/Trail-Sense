package com.kylecorry.trail_sense.plugins.infrastructure.persistence

import com.kylecorry.luna.concurrency.onIO

class PluginRegistrationRepo(private val dao: PluginRegistrationDao) {

    suspend fun get(packageId: String): PluginRegistrationEntity? = onIO {
        dao.get(packageId)
    }

    suspend fun upsert(registration: PluginRegistrationEntity) = onIO {
        dao.upsert(registration)
    }

    suspend fun deleteByPackageId(packageId: String) = onIO {
        dao.deleteByPackageId(packageId)
    }

}
