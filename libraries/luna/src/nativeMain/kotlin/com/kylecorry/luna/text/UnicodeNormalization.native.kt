package com.kylecorry.luna.text

import platform.Foundation.NSString
import platform.Foundation.decomposedStringWithCanonicalMapping

@Suppress("CAST_NEVER_SUCCEEDS")
internal actual fun String.normalizeNfd(): String = (this as NSString).decomposedStringWithCanonicalMapping
