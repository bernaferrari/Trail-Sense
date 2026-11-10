package com.kylecorry.andromeda.alerts

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kylecorry.andromeda.alerts.loading.LoadingIndicator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private class AlertRequest(
    val title: String,
    val message: String,
    val confirm: String,
    val cancel: String,
    val response: CompletableDeferred<Boolean>,
)

/** Owns the currently displayed dialog. Place [AlertHost] in the Compose hierarchy. */
public class AlertState : AlertPresenter {
    private val mutex = Mutex()
    private var request by mutableStateOf<AlertRequest?>(null)

    override suspend fun confirm(title: String, message: String, confirm: String, cancel: String): Boolean =
        mutex.withLock {
            val next = AlertRequest(title, message, confirm, cancel, CompletableDeferred())
            withContext(Dispatchers.Main) { request = next }
            try {
                next.response.await()
            } finally {
                withContext(NonCancellable + Dispatchers.Main) {
                    if (request === next) request = null
                }
            }
        }

    @Composable
    internal fun Render() {
        val current = request ?: return
        AlertDialog(
            onDismissRequest = { current.response.complete(false) },
            title = { Text(current.title) },
            text = { Text(current.message) },
            confirmButton = { TextButton(onClick = { current.response.complete(true) }) { Text(current.confirm) } },
            dismissButton = { TextButton(onClick = { current.response.complete(false) }) { Text(current.cancel) } },
        )
    }
}

@Composable
public fun rememberAlertState(): AlertState = remember { AlertState() }

@Composable
public fun AlertHost(state: AlertState): Unit = state.Render()

public class LoadingState : LoadingIndicator {
    public var visible: Boolean by mutableStateOf(false)
        private set
    override fun show() { visible = true }
    override fun hide() { visible = false }
}

@Composable
public fun rememberLoadingState(): LoadingState = remember { LoadingState() }

@Composable
public fun LoadingIndicator(state: LoadingState, modifier: Modifier = Modifier) {
    if (state.visible) CircularProgressIndicator(modifier)
}
