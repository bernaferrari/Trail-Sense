package com.kylecorry.trail_sense.shared.views

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.settings.infrastructure.IErrorPreferences
import com.kylecorry.trail_sense.shared.text.SystemString
import com.kylecorry.trail_sense.shared.text.systemString
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.ErrorBannerReason
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.icons.TrailSenseIcons
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

/** The app-wide error banner, or null where the platform shell has none (screens then skip reporting). */
val LocalErrorBanner = staticCompositionLocalOf<ErrorBannerState?> { null }

class ErrorBannerState(private val prefs: IErrorPreferences = getAppService<UserPreferences>().errors) {

    private val lock = SynchronizedObject()
    private val errors: MutableList<UserError> = mutableListOf()
    private var currentError by mutableStateOf<UserError?>(null)

    fun report(error: UserError) {
        if (!prefs.canShowError(error.reason)) {
            return
        }
        synchronized(lock) {
            errors.removeAll { it.reason == error.reason }
            errors.add(error)
            errors.sortBy { it.reason.id }
        }
        displayNextError()
    }

    fun dismiss(reason: ErrorBannerReason) {
        synchronized(lock) {
            errors.removeAll { it.reason == reason }
        }
        displayNextError()
    }

    fun dismissAll() {
        synchronized(lock) {
            errors.clear()
        }
        displayNextError()
    }

    private fun displayNextError() {
        val first = synchronized(lock) {
            errors.firstOrNull()
        }
        if (first != null) {
            currentError = first
        } else {
            currentError = null
        }
    }

    @Composable
    fun Content() {
        ErrorBannerContent(
            error = currentError,
            onAction = { currentError?.onAction?.invoke() },
            onClose = {
                currentError?.reason?.let(::dismiss)
            },
            onBannerClick = {
                currentError
                    ?.takeIf { it.action == null }
                    ?.onAction
                    ?.invoke()
            }
        )
    }
}

@Composable
private fun ErrorBannerContent(
    error: UserError?,
    onAction: () -> Unit,
    onClose: () -> Unit,
    onBannerClick: () -> Unit
) {
    if (error == null) {
        return
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (error.action.isNullOrEmpty()) {
                    Modifier.clickable(onClick = onBannerClick)
                } else {
                    Modifier
                }
            ),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 64.dp)
                .padding(start = TrailSenseSpacing.lg, end = TrailSenseSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = TrailSenseIcons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = error.title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            val action = error.action
            if (!action.isNullOrEmpty()) {
                TextButton(onClick = onAction) {
                    Text(text = action)
                }
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = TrailSenseIcons.Filled.Close,
                    contentDescription = systemString(SystemString.Cancel)
                )
            }
        }
    }
}
