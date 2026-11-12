package com.kylecorry.trail_sense.shared.debugging

import com.kylecorry.andromeda.core.tryOrLog

/**
 * The running build, as reported by the app when it starts. Until then it reads as a release build, so debug-only
 * behavior stays off.
 */
object AppBuild {
    var isDebug: Boolean = false
        private set

    var buildType: String = "release"
        private set

    fun configure(isDebug: Boolean, buildType: String) {
        this.isDebug = isDebug
        this.buildType = buildType
    }
}

fun isDebug(): Boolean {
    return AppBuild.isDebug
}

fun ifDebug(fn: () -> Unit) {
    if (isDebug()) {
        tryOrLog(fn)
    }
}

fun getBuildType(): String {
    return AppBuild.buildType
}

fun isPlayStoreBuild(): Boolean {
    return getBuildType() == "playStore"
}
