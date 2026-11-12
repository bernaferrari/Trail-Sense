package com.kylecorry.trail_sense.shared.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.kylecorry.trail_sense.main.getAppService
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource

/** The app's strings in composition; the registered [AppStrings] unless a preview or test provides its own. */
val LocalAppStrings: ProvidableCompositionLocal<AppStrings> = staticCompositionLocalOf { getAppService() }

/**
 * An app string in composition, formatted like Android's getString (%s, %d, %.1f, %1$s) and in the app's language.
 * Prefer this over Compose's stringResource for app strings: it keeps Android's per-app language and placeholders.
 */
@Composable
fun appString(resource: StringResource, vararg args: Any?): String {
    return LocalAppStrings.current.get(resource, *args)
}

/** An app plural string in composition, chosen for [quantity] and formatted like [appString]. */
@Composable
fun appPluralString(resource: PluralStringResource, quantity: Int, vararg args: Any?): String {
    return LocalAppStrings.current.plural(resource, quantity, *args)
}
