package com.kylecorry.andromeda.csv

public class CsvSerializer {
    public fun serialize(rows: List<List<String>>): ByteArray = CSVConvert.toCSV(rows).encodeToByteArray()
    public fun deserialize(bytes: ByteArray): List<List<String>> = CSVConvert.parse(bytes.decodeToString())
}
