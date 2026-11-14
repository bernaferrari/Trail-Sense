package com.kylecorry.andromeda.core.time

import platform.Foundation.NSProcessInfo

internal actual fun monotonicNanos(): Long = (NSProcessInfo.processInfo.systemUptime * 1_000_000_000).toLong()
