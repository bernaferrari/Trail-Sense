package com.kylecorry.trail_sense.tools.navigation.infrastructure.persistence

import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

import android.content.Context
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.main.persistence.AndroidAppDatabase
import com.kylecorry.trail_sense.main.persistence.ICleanable
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationBearing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NavigationBearingService(private val dao: NavigationBearingDao) : ICleanable {

    private val mutex = Mutex()

    suspend fun setBearing(bearing: NavigationBearing?): Unit = mutex.withLock {
        onIO {
            dao.clearActiveBearing()
            if (bearing != null) {
                val entity = NavigationBearingEntity.from(bearing.copy(isActive = true))
                dao.upsert(entity)
            }
        }
    }

    fun getBearing(): Flow<NavigationBearing?> {
        return dao.getActiveBearingFlow().map { it?.toNavigationBearing() }
    }

    override suspend fun clean() {
        dao.deleteOlderThan(Clock.System.now().minus(BEARING_HISTORY_DURATION).toEpochMilliseconds())
    }

    companion object {
        private var instance: NavigationBearingService? = null
        private var BEARING_HISTORY_DURATION = 12.hours

        @Synchronized
        fun getInstance(context: Context): NavigationBearingService {
            if (instance == null) {
                instance = NavigationBearingService(AndroidAppDatabase.getInstance(context).bearingDao())
            }
            return instance!!
        }
    }

}
