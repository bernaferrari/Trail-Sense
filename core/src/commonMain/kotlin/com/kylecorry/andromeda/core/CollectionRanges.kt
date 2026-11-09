package com.kylecorry.andromeda.core

/** Returns the smallest closed range containing the values, or null for an empty collection. */
fun <T : Comparable<T>> Iterable<T>.rangeOrNull(): ClosedRange<T>? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null
    var minimum = iterator.next()
    var maximum = minimum
    while (iterator.hasNext()) {
        val value = iterator.next()
        if (value < minimum) minimum = value
        if (value > maximum) maximum = value
    }
    return minimum..maximum
}
