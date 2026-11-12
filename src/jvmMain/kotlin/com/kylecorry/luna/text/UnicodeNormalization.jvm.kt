package com.kylecorry.luna.text

import java.text.Normalizer

internal actual fun String.normalizeNfd(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
