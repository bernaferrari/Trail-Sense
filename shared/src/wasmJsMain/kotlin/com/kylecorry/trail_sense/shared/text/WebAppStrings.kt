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

/** Reads and caches shared Compose strings synchronously after [preload] runs during Web startup. */
class WebAppStrings : AppStrings {
    private val strings = mutableMapOf<StringResource, String>()
    private val arrays = mutableMapOf<StringArrayResource, List<String>>()
    private val plurals = mutableMapOf<PluralStringResource, Map<Int, String>>()

    override val language: String
        get() = Locale.current.language

    suspend fun preload() {
        for (resource in Res.allStringResources.values) {
            strings[resource] = getString(resource)
        }
        for (resource in Res.allStringArrayResources.values) {
            arrays[resource] = getStringArray(resource)
        }
        val sampleQuantities = listOf(0, 1, 2, 3, 11)
        for (resource in Res.allPluralStringResources.values) {
            plurals[resource] = sampleQuantities.associateWith { quantity ->
                getPluralString(resource, quantity)
            }
        }
    }

    override fun get(resource: StringResource, vararg args: Any?): String {
        val template = strings[resource] ?: resource.key
        return AndroidStringFormat.format(template, *args)
    }

    override fun plural(resource: PluralStringResource, quantity: Int, vararg args: Any?): String {
        val templates = plurals[resource]
        val template = when {
            templates == null -> resource.key
            quantity == 0 -> templates[0] ?: templates[2] ?: resource.key
            quantity == 1 -> templates[1] ?: resource.key
            quantity == 2 -> templates[2] ?: resource.key
            quantity in 3..10 -> templates[3] ?: templates[2] ?: resource.key
            else -> templates[11] ?: templates[2] ?: resource.key
        }
        return AndroidStringFormat.format(template, *args)
    }

    override fun array(resource: StringArrayResource): List<String> = arrays[resource] ?: emptyList()

    override fun system(string: SystemString): String = webSystemString(string)
}
