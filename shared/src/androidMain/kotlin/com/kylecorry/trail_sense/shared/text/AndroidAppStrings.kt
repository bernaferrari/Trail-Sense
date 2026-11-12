package com.kylecorry.trail_sense.shared.text

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.andromeda.core.system.ResourceCache
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import java.util.concurrent.ConcurrentHashMap

/**
 * Reads shared strings from the app's own resources, by name, so Android's per-app language, plural rules and
 * formatting apply exactly as with R.string. The app keeps these resources through shrinking (see the
 * generated shared_strings_keep.xml).
 */
class AndroidAppStrings(context: Context) : AppStrings {

    private val context = context.applicationContext
    private val cache = ResourceCache(this.context.resources)
    private val ids = ConcurrentHashMap<String, Int>()

    override fun get(resource: StringResource, vararg args: Any?): String {
        val id = id(resource.key, "string")
        // As String.format, a null argument reads "null"
        return if (args.isEmpty()) cache.getString(id) else cache.getString(id, *args.map { it ?: "null" }.toTypedArray())
    }

    override fun plural(resource: PluralStringResource, quantity: Int, vararg args: Any?): String {
        return context.resources.getQuantityString(id(resource.key, "plurals"), quantity, *args)
    }

    @SuppressLint("DiscouragedApi")
    private fun id(name: String, type: String): Int {
        return ids.getOrPut("$type/$name") {
            context.resources.getIdentifier(name, type, context.packageName).also {
                check(it != 0) { "The app has no $type named $name" }
            }
        }
    }
}
