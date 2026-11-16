@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.sharing.ActionItem
import com.kylecorry.trail_sense.shared.sharing.ActionSheetContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class PendingActionSheet(
    val title: String,
    val subtitle: String?,
    val actions: List<ActionItem>,
    val stats: List<TrailSenseStat>,
    val result: CompletableDeferred<ActionItem?>
)

/**
 * Multiplatform [TrailSenseDestinationHost] backed by [AppHost] snackbars and [TrailSenseSheet] action sheets.
 */
class SharedComposeDestinationHost(
    override val navigator: TrailSenseNavigator,
    override val lifecycleOwner: LifecycleOwner,
    override val route: Route,
    private val appHost: AppHost,
    private val scope: CoroutineScope,
    private val onOpenWidgets: () -> Unit = {},
    private val onReloadTheme: () -> Unit = {}
) : TrailSenseDestinationHost {

    override var screenController: Any? = null

    internal var activeActionSheet by mutableStateOf<PendingActionSheet?>(null)
        private set

    override suspend fun requestPermission(permission: AppPermission): Boolean {
        // Shared host defaults to permitted and lets platform sensor calls handle OS authorization prompts.
        return true
    }

    override fun explainPermissionDenied(permission: AppPermission) {
        showMessage(permission.name)
    }

    override fun showMessage(message: String, long: Boolean) {
        scope.launch {
            appHost.showSnackbar(message, long = long)
        }
    }

    override suspend fun showSnackbar(
        message: String,
        actionLabel: String?,
        long: Boolean
    ): Boolean {
        return appHost.showSnackbar(message, actionLabel, long)
    }

    override suspend fun chooseAction(
        title: String,
        subtitle: String?,
        actions: List<ActionItem>,
        stats: List<TrailSenseStat>
    ): ActionItem? {
        val deferred = CompletableDeferred<ActionItem?>()
        val sheet = PendingActionSheet(
            title = title,
            subtitle = subtitle,
            actions = actions,
            stats = stats,
            result = deferred
        )
        activeActionSheet = sheet
        return try {
            deferred.await()
        } finally {
            if (activeActionSheet === sheet) {
                activeActionSheet = null
            }
        }
    }

    override suspend fun capturePhoto(
        maxWidth: Int?,
        maxHeight: Int?,
        zeroShutterLag: Boolean
    ): String? {
        return null
    }

    override fun openWidgets() {
        onOpenWidgets()
    }

    override fun reloadTheme() {
        onReloadTheme()
    }
}

/** Renders any pending action sheet triggered through [ScreenEffects.chooseAction]. */
@Composable
fun ActionSheetModal(host: SharedComposeDestinationHost) {
    val sheet = host.activeActionSheet ?: return
    TrailSenseSheet(
        onDismissRequest = { sheet.result.complete(null) },
        title = sheet.title
    ) {
        ActionSheetContent(
            subtitle = sheet.subtitle,
            actions = sheet.actions,
            stats = sheet.stats,
            onAction = { sheet.result.complete(it) }
        )
    }
}
