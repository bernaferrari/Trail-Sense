package com.kylecorry.trail_sense.shared.views.compose

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.core.ui.ReactiveComponent
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.luna.hooks.Ref
import kotlin.coroutines.EmptyCoroutineContext

class TrailSenseReactiveComponent(
    private val contextProvider: () -> Context,
    private val activityProvider: () -> Activity,
    private val rootViewProvider: () -> View,
    private val lifecycleOwnerProvider: () -> LifecycleOwner,
    private val viewLifecycleOwnerProvider: () -> LifecycleOwner,
    private val argumentsProvider: () -> Bundle,
    private val onStateChanged: () -> Unit
) : ReactiveComponent {

    private val hooks = Hooks(EmptyCoroutineContext, 17L, false, onStateChanged)
    private val states = mutableMapOf<String, Any?>()
    private val effectCleanups = mutableMapOf<String, () -> Unit>()
    private var currentHookCount = 0

    fun resetHookCursor() {
        currentHookCount = 0
    }

    fun cleanupEffects() {
        effectCleanups.values.forEach { it() }
        effectCleanups.clear()
    }

    override fun useAndroidContext(): Context {
        return contextProvider()
    }

    override fun useEffect(values: Array<out Any?>, action: () -> Unit) {
        val key = "effect_${currentHookCount++}"
        hooks.effect(key, values.copyOf()) {
            action()
        }
    }

    override fun useEffectWithCleanup(values: Array<out Any?>, action: () -> () -> Unit) {
        val key = "effect_${currentHookCount++}"
        hooks.effect(key, values.copyOf()) {
            effectCleanups.remove(key)?.invoke()
            effectCleanups[key] = action()
        }
    }

    override fun <T> useMemo(values: Array<out Any?>, value: () -> T): T {
        val key = "memo_${currentHookCount++}"
        return hooks.memo(key, values.copyOf(), value = value)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> useState(initialValue: T): Pair<T, (T) -> Unit> {
        val key = "state_${currentHookCount++}"
        if (!states.containsKey(key)) {
            states[key] = initialValue
        }
        return states[key] as T to {
            states[key] = it
            onStateChanged()
        }
    }

    override fun <T> useView(id: Int): T {
        @Suppress("UNCHECKED_CAST")
        return rootViewProvider().findViewById<View>(id) as T
    }

    override fun <T> useRef(initialValue: T): Ref<T> {
        val key = "ref_${currentHookCount++}"
        return hooks.ref(key, initialValue)
    }

    override fun useRootView(): View {
        return rootViewProvider()
    }

    override fun useLifecycleOwner(viewOwner: Boolean): LifecycleOwner {
        return if (viewOwner) {
            viewLifecycleOwnerProvider()
        } else {
            lifecycleOwnerProvider()
        }
    }

    override fun useArguments(): Bundle {
        return argumentsProvider()
    }

    override fun useActivity(): Activity {
        return activityProvider()
    }
}
