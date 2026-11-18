package com.kylecorry.trail_sense.ios

import com.kylecorry.trail_sense.main.SharedAppAction
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

private val actions = Channel<SharedAppAction>(Channel.BUFFERED)
internal val iosAppActions = actions.receiveAsFlow()

fun requestStartBacktrack() {
    actions.trySend(SharedAppAction.StartBacktrack)
}

fun openBacktrack() {
    actions.trySend(SharedAppAction.OpenBacktrack)
}

fun requestSaveCurrentPlace() {
    actions.trySend(SharedAppAction.SaveCurrentPlace)
}
