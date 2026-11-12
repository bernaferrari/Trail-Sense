package com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence

import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.tools.lightning.domain.LightningStrike

class LightningRepo private constructor(private val dao: LightningStrikeDao) : ILightningRepo {

    override suspend fun clean() = onIO {
        dao.deleteOlderThan(Clock.System.now().minus(LIGHTNING_HISTORY_DURATION).toEpochMilliseconds())
    }

    override suspend fun add(reading: Reading<LightningStrike>): Long = onIO {
        val entity = LightningStrikeEntity.from(reading)

        val id = if (entity.id != 0L) {
            dao.update(entity)
            entity.id
        } else {
            dao.insert(entity)
        }
        id
    }

    override suspend fun delete(reading: Reading<LightningStrike>) = onIO {
        val entity = LightningStrikeEntity.from(reading)
        dao.delete(entity)
    }

    override suspend fun get(id: Long): Reading<LightningStrike>? = onIO {
        dao.get(id)?.toReading()
    }

    override suspend fun getLast(): Reading<LightningStrike>? = onIO {
        dao.getLast()?.toReading()
    }

    override suspend fun getAll(): List<Reading<LightningStrike>> = onIO {
        dao.getAllSync().map { it.toReading() }
    }

    override fun getAllLive(): Flow<List<Reading<LightningStrike>>> {
        return dao.getAll().map {
            it.map { reading -> reading.toReading() }
        }
    }

    companion object {
        private val LIGHTNING_HISTORY_DURATION = 2.hours

        private var instance: LightningRepo? = null

        @Synchronized
        fun getInstance(context: Context): LightningRepo {
            if (instance == null) {
                instance = LightningRepo(
                    AppDatabase.getInstance(context.applicationContext).lightningDao()
                )
            }
            return instance!!
        }

    }
}
