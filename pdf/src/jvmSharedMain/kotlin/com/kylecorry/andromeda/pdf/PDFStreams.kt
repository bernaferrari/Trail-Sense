package com.kylecorry.andromeda.pdf

import com.kylecorry.luna.streams.readText
import java.io.InputStream
import java.io.OutputStream

class PDFInputStream(val stream: InputStream) : PDFValue {
    override fun equals(other: Any?) = other is PDFInputStream && stream == other.stream
    override fun hashCode() = stream.hashCode()
    override fun toString() = "stream\n${stream.readText()}\nendstream"
    override fun toByteArray() = "stream\n".encodeToByteArray() + stream.readBytes() + "\nendstream".encodeToByteArray()
}

fun PDFValue.write(output: OutputStream): Long = when (this) {
    is PDFInputStream -> {
        val header = "stream\n".encodeToByteArray()
        val footer = "\nendstream".encodeToByteArray()
        output.write(header)
        val size = stream.copyTo(output)
        output.write(footer)
        header.size + size + footer.size
    }
    is PDFValue.PDFObject -> {
        val header = "$id $generation obj\n".encodeToByteArray()
        val separator = "\n".encodeToByteArray()
        val footer = "endobj\n".encodeToByteArray()
        output.write(header)
        var size = header.size.toLong()
        for (value in content) {
            size += value.write(output)
            output.write(separator)
            size += separator.size
        }
        output.write(footer)
        size + footer.size
    }
    else -> {
        val bytes = toByteArray()
        output.write(bytes)
        bytes.size.toLong()
    }
}
