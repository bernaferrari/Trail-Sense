package com.kylecorry.trail_sense.shared.preferences

import com.kylecorry.andromeda.preferences.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private val changeScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

/**
 * The key of every preference that changes in this store, from andromeda's [Preferences.observeChanges].
 * Stores that cannot report changes (such as in-memory test stores) never emit.
 */
val Preferences.onChange: Flow<String>
    get() = observeChanges()

/**
 * Calls [action] with the key of every preference that changes until the returned job is cancelled, for app-wide
 * objects that have no lifecycle of their own. The listener is registered before this returns.
 */
fun Preferences.onEachChange(action: (String) -> Unit): Job =
    changeScope.launch(start = CoroutineStart.UNDISPATCHED) {
        observeChanges().collect { action(it) }
    }
