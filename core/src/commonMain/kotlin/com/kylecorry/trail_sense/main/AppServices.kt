package com.kylecorry.trail_sense.main

import com.kylecorry.andromeda.core.cache.DependencyRegistry

/** The app-wide instance of [T], registered at startup by the platform's service register. */
inline fun <reified T : Any> getAppService(): T {
    return DependencyRegistry.get()
}
