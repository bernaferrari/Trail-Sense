package com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence

import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.topics.ITopic
import com.kylecorry.luna.topics.Topic
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.main.persistence.IReadingRepo

class CloudRepo private constructor(private val dao: CloudReadingDao) :
    IReadingRepo<CloudObservation> {

    private val _readingsChanged = Topic()
    val readingsChanged: ITopic = _readingsChanged

    override suspend fun clean() = onIO {
        dao.deleteOlderThan(Clock.System.now().minus(CLOUD_HISTORY_DURATION).toEpochMilliseconds())

        // TODO: Only do this if there was a change
        _readingsChanged.publish()
    }

    override suspend fun add(reading: Reading<CloudObservation>): Long = onIO {
        val entity = CloudReadingEntity.from(reading)

        val id = if (entity.id != 0L) {
            dao.update(entity)
            entity.id
        } else {
            dao.insert(entity)
        }
        _readingsChanged.publish()
        id
    }

    override suspend fun delete(reading: Reading<CloudObservation>) = onIO {
        val entity = CloudReadingEntity.from(reading)
        dao.delete(entity)
        _readingsChanged.publish()
    }

    override suspend fun get(id: Long): Reading<CloudObservation>? = onIO {
        dao.get(id)?.toReading()
    }

    override suspend fun getAll(): List<Reading<CloudObservation>> = onIO {
        dao.getAllSync().map { it.toReading() }
    }

    override fun getAllLive(): LiveData<List<Reading<CloudObservation>>> {
        return dao.getAll().map {
            it.map { reading -> reading.toReading() }
        }
    }

    companion object {

        private val CLOUD_HISTORY_DURATION = 2.days

        private var instance: CloudRepo? = null

        @Synchronized
        fun getInstance(context: Context): CloudRepo {
            if (instance == null) {
                instance = CloudRepo(AppDatabase.getInstance(context).cloudDao())
            }
            return instance!!
        }
    }
}
