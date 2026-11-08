package com.kylecorry.andromeda.pdf

/** Parses a PDF into its indirect objects. */
expect fun parsePDF(bytes: ByteArray, ignoreStreams: Boolean = false): List<PDFValue.PDFObject>

/** Writes indirect objects as a complete PDF, including a cross-reference table. */
fun createPDF(objects: List<PDFValue.PDFObject>): ByteArray {
    val ordered = objects.sortedBy { it.id }
    require(ordered.all { it.id > 0 } && ordered.map { it.id }.distinct().size == ordered.size)
    val root = ordered.getByProperty("/Type", PDFValue.PDFName("/Catalog")).first().reference
    val parts = mutableListOf<ByteArray>()
    var size = 0
    fun append(bytes: ByteArray) { parts.add(bytes); size += bytes.size }
    fun append(text: String) = append(text.encodeToByteArray())
    append("%PDF-1.3\n")
    val byId = ordered.associateBy { it.id }
    val offsets = mutableMapOf<Int, Int>()
    ordered.forEach { obj ->
        offsets[obj.id] = size
        append(obj.toByteArray())
    }
    val xrefOffset = size
    val count = (ordered.lastOrNull()?.id ?: 0) + 1
    append("xref\n0 $count\n0000000000 65535 f \n")
    for (id in 1 until count) {
        val offset = offsets[id]
        if (offset == null) append("0000000000 00000 f \n")
        else {
            val generation = byId.getValue(id).generation
            append("${offset.toString().padStart(10, '0')} ${generation.toString().padStart(5, '0')} n \n")
        }
    }
    append("trailer\n<< /Size $count /Root $root >>\nstartxref\n$xrefOffset\n%%EOF")
    val result = ByteArray(size)
    var offset = 0
    parts.forEach { part -> part.copyInto(result, offset); offset += part.size }
    return result
}
