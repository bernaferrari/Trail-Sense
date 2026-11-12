package com.kylecorry.trail_sense.shared.map_layers.ui.layers

/**
 * A read-only set of typed values for a map layer: its preferences, or the parameters of one load.
 * A getter returns its default when the key is missing or holds another type, as Android's Bundle did.
 */
class MapLayerValues(values: Map<String, Any?> = emptyMap()) {

    private val values: Map<String, Any?> = values.toMap()

    val keys: Set<String>
        get() = values.keys

    fun isEmpty(): Boolean = values.isEmpty()

    fun containsKey(key: String): Boolean = values.containsKey(key)

    operator fun get(key: String): Any? = values[key]

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean =
        values[key] as? Boolean ?: defaultValue

    fun getInt(key: String, defaultValue: Int = 0): Int = values[key] as? Int ?: defaultValue

    fun getLong(key: String, defaultValue: Long = 0L): Long = values[key] as? Long ?: defaultValue

    fun getFloat(key: String, defaultValue: Float = 0f): Float = values[key] as? Float ?: defaultValue

    fun getDouble(key: String, defaultValue: Double = 0.0): Double =
        values[key] as? Double ?: defaultValue

    fun getString(key: String): String? = values[key] as? String

    fun getString(key: String, defaultValue: String): String = values[key] as? String ?: defaultValue

    fun getValues(key: String): MapLayerValues? = values[key] as? MapLayerValues

    /** A copy with [key] set to [value], or removed when [value] is null. */
    fun with(key: String, value: Any?): MapLayerValues {
        val copy = values.toMutableMap()
        if (value == null) copy.remove(key) else copy[key] = value
        return MapLayerValues(copy)
    }

    /** A copy with each of [defaults] that is not already set. */
    fun withDefaults(defaults: Map<String, Any?>): MapLayerValues {
        val missing = defaults.filterKeys { it !in values }.filterValues { it != null }
        return if (missing.isEmpty()) this else MapLayerValues(values + missing)
    }

    fun toMap(): Map<String, Any?> = values

    override fun equals(other: Any?): Boolean = other is MapLayerValues && other.values == values

    override fun hashCode(): Int = values.hashCode()

    override fun toString(): String = "MapLayerValues($values)"

    companion object {
        val Empty = MapLayerValues()

        fun of(vararg pairs: Pair<String, Any?>): MapLayerValues =
            MapLayerValues(pairs.filter { it.second != null }.toMap())
    }
}
