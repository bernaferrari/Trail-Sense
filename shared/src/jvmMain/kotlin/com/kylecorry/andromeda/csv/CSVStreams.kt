package com.kylecorry.andromeda.csv

import java.io.InputStream
import java.io.OutputStream

fun CSVConvert.parse(input: InputStream): List<List<String>> = parse(input.bufferedReader().readText())
fun CSVConvert.toCSV(stream: OutputStream, data: List<List<Any?>>) {
    stream.write(toCSV(data).toByteArray())
}
