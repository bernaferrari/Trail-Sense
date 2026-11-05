package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.runtime.staticCompositionLocalOf
import com.kylecorry.trail_sense.shared.sharing.ActionItem

/** Runtime permissions and special access a screen can ask for. */
enum class AppPermission {
    Camera,
    ActivityRecognition,
    ExactAlarms,
    /** Location (and notifications) for Backtrack, with its explanation first. */
    Backtrack,
    BackgroundLocation,
    IgnoreBatteryOptimizations
}

/**
 * What a screen asks of the platform: permissions, a passing message, a snackbar, the action sheet, a photo. Plain
 * Kotlin signatures (suspend, no Android types), so a screen's logic reads the same on any platform; Android
 * implements it in the destination host. Screens get it from [LocalScreenEffects].
 */
interface ScreenEffects {
    /** Asks for [permission] (explaining it first where the app does) and returns whether it was granted. */
    suspend fun requestPermission(permission: AppPermission): Boolean

    /** Tells the person [permission] is needed and how to grant it, after a refusal. */
    fun explainPermissionDenied(permission: AppPermission)

    /** A short message that doesn't need an answer. */
    fun showMessage(message: String, long: Boolean = false)

    /** A snackbar; returns true when its action was pressed. */
    suspend fun showSnackbar(message: String, actionLabel: String? = null, long: Boolean = false): Boolean

    /** The family's action sheet; returns the chosen action, or null when dismissed. */
    suspend fun chooseAction(title: String, subtitle: String? = null, actions: List<ActionItem>): ActionItem?

    /** Takes or picks a photo and returns its URI as a string, or null when cancelled. */
    suspend fun capturePhoto(maxWidth: Int? = null, maxHeight: Int? = null, zeroShutterLag: Boolean = false): String?
}

val LocalScreenEffects = staticCompositionLocalOf<ScreenEffects> {
    error("LocalScreenEffects is only available inside a screen drawn by TrailSenseNavDisplay")
}
