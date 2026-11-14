package com.kylecorry.andromeda.csv

import kotlin.test.Test
import kotlin.test.assertEquals

class CsvRoundTripTest {
    @Test
    fun preservesFinalEmptyCellsAndBlankLines() {
        assertEquals(listOf(listOf("a", "")), CSVConvert.parse("a,"))
        assertEquals(listOf(listOf("")), CSVConvert.parse("\"\""))
        assertEquals(listOf(listOf("a"), listOf(""), listOf("b")), CSVConvert.parse("a\n\nb"))
        val rows = listOf(listOf("one", ""), listOf("two", "a,b"))
        assertEquals(rows, CSVConvert.parse(CSVConvert.toCSV(rows)))
    }
}
