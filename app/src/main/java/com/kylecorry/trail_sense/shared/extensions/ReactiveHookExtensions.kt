package com.kylecorry.trail_sense.shared.extensions

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.ui.ReactiveComponent
import com.kylecorry.luna.topics.ITopic
import com.kylecorry.luna.topics.generic.ITopic as GenericTopic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

fun ReactiveComponent.useBackgroundEffect(
    vararg values: Any?,
    state: BackgroundMinimumState = BackgroundMinimumState.Resumed,
    viewOwner: Boolean = true,
    cancelWhenRerun: Boolean = false,
    block: suspend CoroutineScope.() -> Unit
) {
    val owner = useLifecycleOwner(viewOwner)
    val lastJob = useRef<Job?>(null)
    useEffectWithCleanup(arrayOf(owner, state, viewOwner, cancelWhenRerun, *values)) {
        if (cancelWhenRerun) {
            lastJob.current?.cancel()
        }
        val job = owner.launchRepeating(state, block)
        lastJob.current = job
        return@useEffectWithCleanup {
            job.cancel()
        }
    }
}

fun <T> ReactiveComponent.useFlow(
    flow: Flow<T>,
    vararg values: Any?,
    state: BackgroundMinimumState = BackgroundMinimumState.Resumed,
    context: CoroutineContext = Dispatchers.Default,
    observeOn: CoroutineContext = Dispatchers.Main,
    viewOwner: Boolean = true
): T? {
    val (value, setValue) = useState<T?>(null)
    val owner = useLifecycleOwner(viewOwner)
    useEffectWithCleanup(arrayOf(flow, owner, state, context, observeOn, viewOwner, *values)) {
        val job = owner.launchRepeating(state) {
            kotlinx.coroutines.withContext(context) {
                flow.collect {
                    kotlinx.coroutines.withContext(observeOn) {
                        setValue(it)
                    }
                }
            }
        }
        return@useEffectWithCleanup {
            job.cancel()
        }
    }
    return value
}

fun <T : ITopic, V> ReactiveComponent.useTopic(
    topic: T,
    default: V,
    mapper: (T) -> V
): V {
    val (value, setValue) = useState(default)
    val owner = useLifecycleOwner()
    val currentMapper = useRef(mapper)
    currentMapper.current = mapper
    useEffectWithCleanup(arrayOf(topic, owner)) {
        val callback = {
            setValue(currentMapper.current(topic))
            true
        }
        topic.subscribe(callback)
        return@useEffectWithCleanup {
            topic.unsubscribe(callback)
        }
    }
    return value
}

fun <T : ITopic, V> ReactiveComponent.useTopic(
    topic: T,
    mapper: (T) -> V
): V {
    return useTopic(topic, mapper(topic), mapper)
}

fun <T : Any, V> ReactiveComponent.useTopic(
    topic: GenericTopic<T>,
    default: V,
    mapper: (T) -> V
): V {
    val (value, setValue) = useState(default)
    val owner = useLifecycleOwner()
    val currentMapper = useRef(mapper)
    currentMapper.current = mapper
    useEffectWithCleanup(arrayOf(topic, owner)) {
        val callback = { updated: T ->
            setValue(currentMapper.current(updated))
            true
        }
        topic.subscribe(callback)
        return@useEffectWithCleanup {
            topic.unsubscribe(callback)
        }
    }
    return value
}

fun <T : Any, V> ReactiveComponent.useTopic(
    topic: GenericTopic<T>,
    mapper: (T) -> V
): V? {
    return topic.value.orElse(null)?.let {
        useTopic(topic, mapper(it), mapper)
    } ?: useTopic(topic, null, mapper)
}

private fun LifecycleOwner.launchRepeating(
    state: BackgroundMinimumState,
    block: suspend CoroutineScope.() -> Unit
): Job {
    return lifecycleScope.launch {
        if (state == BackgroundMinimumState.Any) {
            block()
        } else {
            lifecycle.repeatOnLifecycle(state.toLifecycleState()) {
                block()
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
