package com.kylecorry.trail_sense.shared.extensions

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

fun LifecycleOwner.inBackground(
    state: BackgroundMinimumState = BackgroundMinimumState.Resumed,
    restartWhenResumed: Boolean = true,
    cancelWhenRerun: Boolean = true,
    block: suspend CoroutineScope.() -> Unit
): Job {
    return lifecycleScope.launch {
        if (state == BackgroundMinimumState.Any) {
            block()
        } else if (restartWhenResumed) {
            lifecycle.repeatOnLifecycle(state.toLifecycleState()) {
                block()
            }
        } else {
            lifecycle.repeatOnLifecycle(state.toLifecycleState()) {
                block()
                if (cancelWhenRerun) {
                    this@launch.cancel()
                }
            }
        }
    }
}

private fun BackgroundMinimumState.toLifecycleState(): Lifecycle.State {
    return when (this) {
        BackgroundMinimumState.Resumed -> Lifecycle.State.RESUMED
        BackgroundMinimumState.Started -> Lifecycle.State.STARTED
        BackgroundMinimumState.Created -> Lifecycle.State.CREATED
        BackgroundMinimumState.Any -> Lifecycle.State.INITIALIZED
    }
}
