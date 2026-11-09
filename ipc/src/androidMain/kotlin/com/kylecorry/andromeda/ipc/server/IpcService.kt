package com.kylecorry.andromeda.ipc.server

import android.app.Service
import android.content.Intent
import android.os.IBinder

abstract class IpcService : Service() {
    abstract val router: MessengerIpcRouter

    override fun onBind(intent: Intent?): IBinder? {
        return router.bind()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        router.unbind()
        return super.onUnbind(intent)
    }
}