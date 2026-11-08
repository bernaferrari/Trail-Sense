package com.kylecorry.andromeda.core.system

import android.net.Uri
import androidx.core.net.toUri

val GeoUri.uri: Uri get() = toString().toUri()
fun GeoUri.Companion.from(uri: Uri): GeoUri? = parse(uri.toString())
