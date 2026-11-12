package com.kylecorry.trail_sense.shared.text

import androidx.compose.ui.text.intl.Locale
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

/** Reads shared strings from Compose resources in the device's language. */
class ComposeAppStrings : AppStrings {

    override val language: String
        get() = Locale.current.language

    override fun get(resource: StringResource, vararg args: Any?): String {
        // Compose resources caches each string after its first read
        val template = runBlocking { getString(resource) }
        return AndroidStringFormat.format(template, *args)
    }

    override fun plural(resource: PluralStringResource, quantity: Int, vararg args: Any?): String {
        val template = runBlocking { getPluralString(resource, quantity) }
        return AndroidStringFormat.format(template, *args)
    }

    override fun system(string: SystemString): String = iosSystemString(string)
}
