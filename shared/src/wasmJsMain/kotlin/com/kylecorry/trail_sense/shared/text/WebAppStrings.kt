package com.kylecorry.trail_sense.shared.text

import androidx.compose.ui.text.intl.Locale
import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.allPluralStringResources
import com.kylecorry.trail_sense.shared.generated.resources.allStringArrayResources
import com.kylecorry.trail_sense.shared.generated.resources.allStringResources
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringArrayResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getStringArray
import kotlin.js.ExperimentalWasmJsInterop

/** Reads and caches shared Compose strings synchronously after [preload] runs during Web startup. */
class WebAppStrings : AppStrings {
    private val strings = mutableMapOf<StringResource, String>()
    private val arrays = mutableMapOf<StringArrayResource, List<String>>()
    private val plurals = mutableMapOf<PluralStringResource, Map<String, String>>()

    override val language: String
        get() = Locale.current.language

    suspend fun preload() {
        for (resource in Res.allStringResources.values) {
            strings[resource] = getString(resource)
        }
        for (resource in Res.allStringArrayResources.values) {
            arrays[resource] = getStringArray(resource)
        }
        val locale = Locale.current.toLanguageTag()
        val samples = ((0..200).toList() + listOf(1_000, 10_000, 100_000, 1_000_000))
            .associateBy { pluralCategory(locale, it) }
            .toMutableMap()
        if ("other" !in samples) samples["other"] = 101
        for (resource in Res.allPluralStringResources.values) {
            plurals[resource] = samples.mapValues { (_, quantity) -> getPluralString(resource, quantity) }
        }
    }

    override fun get(resource: StringResource, vararg args: Any?): String {
        val template = strings[resource] ?: resource.key
        return AndroidStringFormat.format(template, *args)
    }

    override fun plural(resource: PluralStringResource, quantity: Int, vararg args: Any?): String {
        val templates = plurals[resource]
        val category = pluralCategory(Locale.current.toLanguageTag(), quantity)
        val template = templates?.get(category) ?: templates?.get("other") ?: resource.key
        return AndroidStringFormat.format(template, *args)
    }

    override fun array(resource: StringArrayResource): List<String> = arrays[resource] ?: emptyList()

    override fun system(string: SystemString): String = portableSystemString(string)
}


@OptIn(ExperimentalWasmJsInterop::class)
private fun pluralCategory(locale: String, quantity: Int): String = js(
    """{
        const cache = globalThis.trailSensePluralRules ??= new Map();
        if (!cache.has(locale)) cache.set(locale, new Intl.PluralRules(locale));
        return cache.get(locale).select(quantity);
    }"""
)
