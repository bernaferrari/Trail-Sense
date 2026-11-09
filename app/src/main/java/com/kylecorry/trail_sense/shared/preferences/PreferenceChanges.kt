package com.kylecorry.trail_sense.shared.preferences

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.luna.topics.generic.Topic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.WeakHashMap

private val changeTopics = WeakHashMap<Preferences, Topic<String>>()
private val changeScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

/**
 * Publishes the key of every preference that changes in this store, bridging andromeda's
 * [Preferences.observeChanges] flow to the topic-based listeners used throughout the app.
 * Stores that cannot report changes (such as in-memory test stores) never publish.
 */
val Preferences.onChange: Topic<String>
    get() = synchronized(changeTopics) {
        changeTopics.getOrPut(this) {
            lateinit var topic: Topic<String>
            var job: Job? = null
            topic = Topic.lazy(
                { job = changeScope.launch { observeChanges().collect { topic.publish(it) } } },
                { job?.cancel(); job = null }
            )
            topic
        }
    }
