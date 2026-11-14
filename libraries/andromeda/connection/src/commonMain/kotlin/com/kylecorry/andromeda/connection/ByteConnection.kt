package com.kylecorry.andromeda.connection

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** A bidirectional byte stream. Closing it also interrupts pending operations. */
public interface ByteConnection : AutoCloseable {
    /** Reads up to [maxBytes], or returns null when the peer closes the stream. */
    public suspend fun read(maxBytes: Int = 4096): ByteArray?

    /** Writes all bytes to the connection. */
    public suspend fun write(bytes: ByteArray)
}

/** Reads UTF-8 lines, including a final unterminated line. Line terminators are removed. */
public fun ByteConnection.lines(maxLineBytes: Int = 1024 * 1024): Flow<String> = flow {
    require(maxLineBytes > 0)
    var line = ByteArray(minOf(256, maxLineBytes))
    var size = 0
    while (true) {
        val chunk = read() ?: break
        for (byte in chunk) {
            if (byte == 10.toByte()) {
                val length = if (size > 0 && line[size - 1] == 13.toByte()) size - 1 else size
                emit(line.decodeToString(0, length))
                size = 0
            } else {
                check(size < maxLineBytes) { "Line exceeds $maxLineBytes bytes" }
                if (size == line.size) line = line.copyOf(line.size + minOf(line.size, maxLineBytes - line.size))
                line[size++] = byte
            }
        }
    }
    if (size > 0) emit(line.decodeToString(0, size))
}

/** Writes a UTF-8 line terminated by a newline. */
public suspend fun ByteConnection.writeLine(line: String): Unit = write("$line\n".encodeToByteArray())
