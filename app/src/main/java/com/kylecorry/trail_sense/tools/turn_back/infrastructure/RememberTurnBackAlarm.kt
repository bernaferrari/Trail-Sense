package com.kylecorry.trail_sense.tools.turn_back.infrastructure

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.dialog
import com.kylecorry.trail_sense.shared.permissions.IsBackgroundLocationRequired
import com.kylecorry.trail_sense.shared.preferences.PreferencesFlag
import com.kylecorry.trail_sense.shared.text.markdown.MarkdownService
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import com.kylecorry.trail_sense.tools.turn_back.infrastructure.receivers.TurnBackAlarmReceiver
import com.kylecorry.trail_sense.tools.turn_back.ui.TurnBackAlarm
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** The turn back alarm on Android: an exact alarm receiver, plus background location for the sunset lookup. */
@Composable
fun rememberTurnBackAlarm(): TurnBackAlarm {
    val context = LocalContext.current
    val effects = LocalScreenEffects.current
    return remember(context, effects) {
        object : TurnBackAlarm {
            override suspend fun start() {
                // Exact alarms first; the alarm starts whether or not they were granted, as it always has.
                effects.requestPermission(AppPermission.ExactAlarms)
                TurnBackAlarmReceiver.start(context)
                requestBackgroundLocation(context, getAppService<Preferences>(), effects)
            }

            override fun stop() {
                TurnBackAlarmReceiver.stop(context)
            }
        }
    }
}

/** Once, when the alarm needs it, offers background location so the alert can find the sunset while closed. */
private suspend fun requestBackgroundLocation(context: Context, preferences: Preferences, effects: ScreenEffects) {
    val shownFlag = PreferencesFlag(preferences, "cache_background_location_requested")
    if (!IsBackgroundLocationRequired().isSatisfiedBy(context)) {
        shownFlag.set(false)
        return
    }
    if (shownFlag.get()) {
        return
    }

    shownFlag.set(true)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return
    }

    val learnMore = effects.showSnackbar(
        context.getString(R.string.sunset_alerts_background_location_disclaimer),
        actionLabel = context.getString(R.string.learn_more),
        long = true
    )
    if (!learnMore) {
        return
    }
    val markdown = DependencyRegistry.get<MarkdownService>()
    val contents = markdown.toMarkdown(context.getString(R.string.access_background_location_rationale))
    val cancelled = suspendCancellableCoroutine { continuation ->
        Alerts.dialog(
            context,
            context.getString(R.string.access_background_location),
            contents,
            okText = context.getString(R.string.settings),
            allowLinks = true
        ) { cancelled ->
            if (continuation.isActive) continuation.resume(cancelled)
        }
    }
    if (!cancelled) {
        effects.requestPermission(AppPermission.BackgroundLocation)
    }
}

