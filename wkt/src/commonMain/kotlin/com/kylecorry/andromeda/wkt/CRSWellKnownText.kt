package com.kylecorry.andromeda.wkt

public interface WKTValue
public data class WKTString(val value: String) : WKTValue
public data class WKTNumber(val value: Double) : WKTValue
public data class WKTSection(val name: String, val values: List<WKTValue>) : WKTValue {
    public inline fun <reified T : WKTValue> get(idx: Int): T? {
        val value = values.getOrNull(idx)
        if (value is T) {
            return value
        }
        return null
    }
}

public fun WKTValue.getSection(name: String): WKTSection? {
    if (this is WKTSection) {
        if (this.name.equals(name, true)) {
            return this
        }

        for (value in values) {
            val section = value.getSection(name)
            if (section != null) {
                return section
            }
        }
    }
    return null
}