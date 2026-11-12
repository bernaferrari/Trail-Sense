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
    /** The ISO 639 code of the language the strings are read in, such as "en". */
    val language: String

    fun get(resource: StringResource, vararg args: Any?): String
    fun plural(resource: PluralStringResource, quantity: Int, vararg args: Any?): String

    /** The platform's own translation of a common label (composables use systemString). */
    fun system(string: SystemString): String
}
