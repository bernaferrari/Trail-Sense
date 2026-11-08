package com.kylecorry.andromeda.csv

class CsvSerializer {
    fun serialize(rows: List<List<String>>): ByteArray = CSVConvert.toCSV(rows).encodeToByteArray()
    fun deserialize(bytes: ByteArray): List<List<String>> = CSVConvert.parse(bytes.decodeToString())
}
