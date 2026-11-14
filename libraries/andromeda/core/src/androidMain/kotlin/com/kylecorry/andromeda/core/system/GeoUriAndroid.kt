package com.kylecorry.andromeda.core.system

import android.net.Uri
import androidx.core.net.toUri

public val GeoUri.uri: Uri get() = toString().toUri()
public fun GeoUri.Companion.from(uri: Uri): GeoUri? = parse(uri.toString())
