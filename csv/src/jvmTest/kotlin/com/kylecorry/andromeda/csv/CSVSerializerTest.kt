package com.kylecorry.andromeda.csv

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CSVSerializerTest {
    @Test fun roundTrip() {
        val rows = listOf(listOf("Number", "Text"), listOf("1", "Testing, \"123\""))
        val serializer = CsvSerializer()
        assertEquals(rows, serializer.deserialize(serializer.serialize(rows)))
    }
}
