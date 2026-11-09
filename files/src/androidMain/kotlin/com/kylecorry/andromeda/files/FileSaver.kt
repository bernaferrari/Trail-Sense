package com.kylecorry.andromeda.files

import java.io.*

public class FileSaver(private val autoClose: Boolean = true) {

    public fun save(input: InputStream, output: OutputStream) {
        try {
            input.copyTo(output, 1024)
        } finally {
            if (autoClose) {
                try {
                    input.close()
                } catch (e: Exception) {
                    // Do nothing
                }
                try {
                    output.close()
                } catch (e: Exception) {
                    // Do nothing
                }
            }
        }
    }

    public fun save(input: InputStream, output: File) {
        FileOutputStream(output).use {
            save(input, it)
        }
    }

    public fun save(input: File, output: File) {
        FileInputStream(input).use { inputStream ->
            FileOutputStream(output).use { outputStream ->
                save(inputStream, outputStream)
            }
        }
    }

    public fun save(input: File, output: OutputStream) {
        FileInputStream(input).use { inputStream ->
            save(inputStream, output)
        }
    }

}