package com.kylecorry.andromeda.connection

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ByteConnectionTest {
    private class MemoryConnection(private val chunks: Iterator<ByteArray>) : ByteConnection {
        var output = byteArrayOf()
        override suspend fun read(maxBytes: Int): ByteArray? = if (chunks.hasNext()) chunks.next() else null
        override suspend fun write(bytes: ByteArray) { output += bytes }
        override fun close() = Unit
    }

    @Test fun linesHandleSplitUtf8AndTerminators() = runBlocking {
        val bytes = "café\r\nsecond\nlast".encodeToByteArray()
        val connection = MemoryConnection(bytes.map { byteArrayOf(it) }.iterator())
        assertEquals(listOf("café", "second", "last"), connection.lines().toList())
        connection.writeLine("hello")
        assertEquals("hello\n", connection.output.decodeToString())
    }

    @Test fun rejectsOversizedLines(): Unit = runBlocking {
        val connection = MemoryConnection(listOf("abcd".encodeToByteArray()).iterator())
        assertFailsWith<IllegalStateException> { connection.lines(3).toList() }
    }
}
