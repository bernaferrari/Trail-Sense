package com.kylecorry.trail_sense.shared.events

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The app-wide broadcasts tools send each other (a path started recording, a map changed, a widget needs
 * redrawing). Nothing is replayed: subscribers only see broadcasts sent after they subscribe.
 */
object ToolBroadcasts {
    private val broadcasts = MutableSharedFlow<Pair<String, EventData>>(extraBufferCapacity = 64)
    private val lock = SynchronizedObject()
    private val subscriptions = mutableMapOf<Pair<String, (EventData) -> Unit>, Job>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** The broadcasts sent with [broadcastId] from now on. */
    fun flow(broadcastId: String): Flow<EventData> =
        broadcasts.filter { it.first == broadcastId }.map { it.second }

    fun broadcast(broadcastId: String, data: EventData? = null) {
        broadcasts.tryEmit(broadcastId to (data ?: EventData()))
    }

    /** Calls [callback] on the main thread for each broadcast with [broadcastId] until [unsubscribe]. */
    fun subscribe(broadcastId: String, callback: (EventData) -> Unit) {
        val key = broadcastId to callback
        synchronized(lock) {
            if (subscriptions.containsKey(key)) return
            // Undispatched, so the subscription is in place before this returns
            subscriptions[key] = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                flow(broadcastId).collect { callback(it) }
            }
        }
    }

    fun unsubscribe(broadcastId: String, callback: (EventData) -> Unit) {
        synchronized(lock) {
            subscriptions.remove(broadcastId to callback)
        }?.cancel()
    }
}
