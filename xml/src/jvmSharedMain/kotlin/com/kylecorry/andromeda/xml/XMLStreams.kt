package com.kylecorry.andromeda.xml

import java.io.InputStream
import java.io.OutputStream

fun XMLConvert.parse(stream: InputStream, autoClose: Boolean = false): XMLNode =
    if (autoClose) stream.bufferedReader().use { parse(it.readText()) } else parse(stream.bufferedReader().readText())

fun XMLConvert.write(xml: XMLNode, stream: OutputStream, isRoot: Boolean = true) {
    stream.bufferedWriter().use { it.write(toString(xml, isRoot)) }
}
