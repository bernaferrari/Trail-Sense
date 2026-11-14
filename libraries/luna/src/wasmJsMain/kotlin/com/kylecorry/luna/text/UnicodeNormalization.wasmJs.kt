package com.kylecorry.luna.text

private fun jsNormalizeNfd(value: String): String = js("value.normalize('NFD')")

internal actual fun String.normalizeNfd(): String = jsNormalizeNfd(this)
