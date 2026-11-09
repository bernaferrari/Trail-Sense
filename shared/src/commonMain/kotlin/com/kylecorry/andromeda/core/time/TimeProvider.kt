package com.kylecorry.andromeda.core.time

public interface TimeProvider {
    public fun elapsedRealtime(): Long
    public fun currentTimeMillis(): Long
}
