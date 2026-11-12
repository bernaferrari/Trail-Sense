package com.kylecorry.trail_sense.tools.weather.infrastructure.persistence

import kotlin.time.Duration.Companion.hours

import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.kylecorry.luna.concurrency.onIO
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.tools.weather.domain.RawWeatherObservation

class WeatherRepo private constructor(context: Context) : IWeatherRepo {

    private val pressureDao = AppDatabase.getInstance(context).pressureDao()

    private val _readingsChanged =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val readingsChanged: SharedFlow<Unit> = _readingsChanged

    private fun notifyReadingsChanged() {
        _readingsChanged.tryEmit(Unit)
    }

    override suspend fun add(reading: Reading<RawWeatherObservation>): Long = onIO {
        val entity = PressureReadingEntity.from(reading)

        val id = if (entity.id != 0L) {
            pressureDao.update(entity)
            entity.id
        } else {
            pressureDao.insert(entity)
        }
        notifyReadingsChanged()
        id
    }

    override suspend fun delete(reading: Reading<RawWeatherObservation>) = onIO {
        val entity = PressureReadingEntity.from(reading)
        pressureDao.delete(entity)
        notifyReadingsChanged()
    }

    override suspend fun get(id: Long): Reading<RawWeatherObservation>? = onIO {
        pressureDao.get(id)?.toWeatherObservation()
    }

    override suspend fun getAll(): List<Reading<RawWeatherObservation>> = onIO {
        pressureDao.getAllSync().map { it.toWeatherObservation() }
    }

    override fun getAllLive(): Flow<List<Reading<RawWeatherObservation>>> {
        return pressureDao.getAll().map {
            it.map { reading -> reading.toWeatherObservation() }
        }
    }

    override suspend fun getLast(): Reading<RawWeatherObservation>? = onIO {
        pressureDao.getLast()?.toWeatherObservation()
    }

    override suspend fun addAll(value: List<Reading<RawWeatherObservation>>) = onIO {
        val entities = value.map { PressureReadingEntity.from(it) }
        val toAdd = entities.filter { it.id == 0L }
        val toUpdate = entities.filter { it.id != 0L }

        if (toAdd.isNotEmpty()) {
            pressureDao.bulkInsert(toAdd)
        }

        if (toUpdate.isNotEmpty()) {
            pressureDao.bulkUpdate(toUpdate)
        }

        notifyReadingsChanged()
    }

    override suspend fun replaceAll(value: List<Reading<RawWeatherObservation>>) = onIO {
        pressureDao.deleteAll()
        addAll(value)
    }

    override suspend fun clean() = onIO {
        pressureDao.deleteOlderThan(Clock.System.now().minus(PRESSURE_HISTORY_DURATION).toEpochMilliseconds())

        // TODO: Only do this if there was a change
        notifyReadingsChanged()
    }

    companion object {

        private val PRESSURE_HISTORY_DURATION = 2.days.plus(6.hours)

        private var instance: WeatherRepo? = null

        @Synchronized
        fun getInstance(context: Context): WeatherRepo {
            if (instance == null) {
                instance = WeatherRepo(context.applicationContext)
            }
            return instance!!
        }
    }
}
