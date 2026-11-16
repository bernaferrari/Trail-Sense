package com.kylecorry.trail_sense.tools.pedometer.infrastructure.persistence

import kotlin.time.Duration.Companion.milliseconds

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.kylecorry.trail_sense.tools.pedometer.domain.StepCountBucket
import kotlin.time.Instant

@Entity(
    tableName = "step_count_buckets",
    indices = [Index(value = ["period_id"])]
)
data class StepCountBucketEntity(
    @ColumnInfo(name = "period_id") val periodId: Long,
    @ColumnInfo(name = "start_time") val startTime: Instant,
    @ColumnInfo(name = "end_time") val endTime: Instant,
    @ColumnInfo(name = "steps") val steps: Long,
    @ColumnInfo(name = "active_time") val activeTime: Long
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0

    fun toStepCountBucket(): StepCountBucket {
        return StepCountBucket(
            id = id,
            periodId = periodId,
            startTime = startTime,
            endTime = endTime,
            steps = steps,
            activeTime = activeTime.milliseconds
        )
    }

    companion object {
        fun from(bucket: StepCountBucket): StepCountBucketEntity {
            return StepCountBucketEntity(
                periodId = bucket.periodId,
                startTime = bucket.startTime,
                endTime = bucket.endTime,
                steps = bucket.steps,
                activeTime = bucket.activeTime.inWholeMilliseconds
            ).also {
                it.id = bucket.id
            }
        }
    }
}
