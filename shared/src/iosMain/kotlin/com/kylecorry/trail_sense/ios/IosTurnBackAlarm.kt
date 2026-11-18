package com.kylecorry.trail_sense.ios

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.return_time
import com.kylecorry.trail_sense.shared.generated.resources.turn_back_notification_title
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.tools.turn_back.ui.TurnBackAlarm
import com.kylecorry.trail_sense.tools.turn_back.ui.TurnBackKeys
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface IosTurnBackScheduler {
    fun schedule(
        turnBackEpochMillis: Long,
        returnEpochMillis: Long,
        turnBackTitle: String,
        returnTitle: String,
        completion: IosAlarmCompletion
    )

    fun cancel()
}

fun interface IosAlarmCompletion {
    fun complete(scheduled: Boolean)
}

internal class IosTurnBackAlarm(
    private val scheduler: IosTurnBackScheduler,
    private val preferences: Preferences,
    private val strings: AppStrings
) : TurnBackAlarm {
    override suspend fun start(): Boolean {
        val turnBack = preferences.getInstant(TurnBackKeys.PREF_TURN_BACK_TIME) ?: return false
        val returnTime = preferences.getInstant(TurnBackKeys.PREF_TURN_BACK_RETURN_TIME) ?: return false
        return suspendCancellableCoroutine { continuation ->
            scheduler.schedule(
                turnBack.toEpochMilliseconds(),
                returnTime.toEpochMilliseconds(),
                strings.get(Res.string.turn_back_notification_title),
                strings.get(Res.string.return_time)
            ) { success ->
                if (continuation.isActive) continuation.resume(success)
            }
        }
    }

    override fun stop() = scheduler.cancel()
}
