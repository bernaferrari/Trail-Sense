package com.kylecorry.andromeda.background

interface AlwaysOnTaskScheduler: TaskScheduler {
    fun start()
}