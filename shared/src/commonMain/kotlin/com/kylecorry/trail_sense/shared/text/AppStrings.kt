package com.kylecorry.trail_sense.shared.text

import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource

/**
 * The app's strings, read outside composition (composables use stringResource). Each platform reads them from
 * where it keeps them: Android from the app's resources, so its per-app language applies, and other platforms
 * from the copies in Compose resources (see shared-strings.txt).
 *
 * Templates use Android's placeholders (%s, %d, %1$s, %%).
 */
interface AppStrings {
    fun get(resource: StringResource, vararg args: Any?): String
    fun plural(resource: PluralStringResource, quantity: Int, vararg args: Any?): String
}
