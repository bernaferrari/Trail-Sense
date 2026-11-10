package com.kylecorry.andromeda.core

public inline fun tryOrNothing(block: () -> Unit) {
    try {
        block()
    } catch (_: Exception) {
    }
}

public inline fun tryOrLog(block: () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

public inline fun <T> tryOrDefault(default: T, block: () -> T): T {
    return try {
        block()
    } catch (e: Exception) {
        default
    }
}

