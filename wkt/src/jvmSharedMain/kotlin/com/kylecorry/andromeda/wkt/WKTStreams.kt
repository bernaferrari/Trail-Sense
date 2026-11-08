package com.kylecorry.andromeda.wkt

import java.io.InputStream

fun CRSWellKnownTextConvert.toWKT(stream: InputStream): WKTValue? = toWKT(stream.bufferedReader().readText())
