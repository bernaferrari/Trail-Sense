package com.kylecorry.trail_sense.shared.events

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ToolBroadcastsTest {

    @Test
    fun deliversDataToListenersOfThatBroadcast() = runTest {
        val received = async(start = CoroutineStart.UNDISPATCHED) {
            ToolBroadcasts.flow("test-a").first()
        }
        ToolBroadcasts.broadcast("test-b", EventData().apply { putLong("value", 1) })
        ToolBroadcasts.broadcast("test-a", EventData().apply { putLong("value", 2) })
        assertEquals(2L, received.await().getLong("value"))
    }

    @Test
    fun broadcastsWithoutDataArriveEmpty() = runTest {
        val received = async(start = CoroutineStart.UNDISPATCHED) {
            ToolBroadcasts.flow("test-empty").take(2).toList()
        }
        ToolBroadcasts.broadcast("test-empty")
        ToolBroadcasts.broadcast("test-empty")
        val all = received.await()
        assertEquals(2, all.size)
        assertNull(all.first().getString("anything"))
    }
}
