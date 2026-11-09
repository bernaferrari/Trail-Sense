package com.kylecorry.andromeda.core.sensors

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.kylecorry.andromeda.core.system.broadcasts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * A sensor whose readings come from Android broadcasts matching [intentFilter].
 * While started it collects [broadcasts] on the main thread and passes each intent to [handleIntent].
 */
abstract class BaseBroadcastReceiverSensor(
    protected val context: Context,
    private val intentFilter: IntentFilter,
    private val listenToBroadcastsFromOtherApps: Boolean = false,
    private val isStickyBroadcast: Boolean = false
) : SharedSensor() {

    private var collection: Job? = null

    protected abstract fun handleIntent(context: Context, intent: Intent)

    override fun startImpl() {
        collection?.cancel()
        collection = broadcasts(context, intentFilter, listenToBroadcastsFromOtherApps, isStickyBroadcast)
            .onEach(::onReceive)
            .launchIn(CoroutineScope(Dispatchers.Main.immediate))
    }

    override fun stopImpl() {
        collection?.cancel()
        collection = null
    }

    private fun onReceive(intent: Intent) {
        handleIntent(context, intent)
        setEventTimeToNow()
        notifyListeners()
    }
}
