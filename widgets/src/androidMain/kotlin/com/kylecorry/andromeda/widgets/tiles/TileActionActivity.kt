package com.kylecorry.andromeda.widgets.tiles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal object TileActionRegistry {
    private val nextId = AtomicInteger()
    private val actions = ConcurrentHashMap<Int, suspend () -> Unit>()
    fun add(action: suspend () -> Unit): Int = nextId.incrementAndGet().also { actions[it] = action }
    fun take(id: Int): (suspend () -> Unit)? = actions.remove(id)
}

/** A transparent Compose host that lets a tile start work while its app is visible. */
public class TileActionActivity : ComponentActivity() {
    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { }
    }

    override fun onResume() {
        super.onResume()
        if (started) return
        started = true
        val action = TileActionRegistry.take(intent.getIntExtra(ACTION_ID, 0))
        if (action == null) {
            finish()
            return
        }
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.Default) { action() }
            } finally {
                finish()
            }
        }
    }

    internal companion object { const val ACTION_ID = "com.kylecorry.andromeda.widgets.TILE_ACTION_ID" }
}
