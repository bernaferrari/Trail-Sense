package com.kylecorry.luna.text

/** This string in Unicode canonical decomposition (NFD), so accents become separate combining marks. */
internal expect fun String.normalizeNfd(): String
