package com.kylecorry.andromeda.csv

object CSVConvert {
    fun toCSV(data: List<List<Any?>>): String = buildString {
        for (row in data) {
            row.forEachIndexed { index, value ->
                val cell = value?.toString().orEmpty()
                val quote = cell.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
                if (quote) append('"')
                append(cell.replace("\"", "\"\""))
                if (quote) append('"')
                if (index != row.lastIndex) append(',')
            }
            append("\r\n")
        }
    }

    fun parse(csv: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        val currentCell = StringBuilder()
        var insideQuotes = false
        var i = 0
        while (i < csv.length) {
            val char = csv[i]
            when (char) {
                '"' -> {
                    // #5a or #6/7
                    if (insideQuotes && i < csv.lastIndex && csv[i + 1] == '"') {
                        currentCell.append('"')
                        i++
                    } else {
                        insideQuotes = !insideQuotes
                    }
                }

                ',' -> {
                    // #4a or #6
                    if (insideQuotes) {
                        currentCell.append(char)
                    } else {
                        currentRow.add(currentCell.toString())
                        currentCell.clear()
                    }
                }

                '\r', '\n' -> {
                    // #1 or #6
                    if (insideQuotes) {
                        currentCell.append(char)
                    } else {
                        if (char == '\r' && i < csv.lastIndex && csv[i + 1] == '\n') {
                            // Also skip the LF in CRLF
                            i++
                        }
                        currentRow.add(currentCell.toString())
                        rows.add(currentRow)
                        currentRow = mutableListOf()
                        currentCell.clear()
                    }
                }

                else -> {
                    currentCell.append(char)
                }
            }
            i++
        }

        // A final delimiter or quoted empty value still represents a cell.
        if (csv.isNotEmpty() && csv.last() != '\n' && csv.last() != '\r') {
            currentRow.add(currentCell.toString())
            rows.add(currentRow)
        } else if (insideQuotes) {
            currentRow.add(currentCell.toString())
            rows.add(currentRow)
        }

        return rows
    }

}
