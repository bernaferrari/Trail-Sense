package com.kylecorry.andromeda.files

import android.net.Uri
import java.io.File
import java.io.InputStream
import java.io.OutputStream

public interface IFileSystem {
    public fun getUri(path: String, authority: String, create: Boolean = false): Uri
    public fun getUri(path: String, create: Boolean = false): Uri
    public fun createFile(path: String)
    public fun createDirectory(path: String)
    public fun delete(path: String, recursive: Boolean = false)
    public fun getDirectory(path: String, create: Boolean = true): File
    public fun getFile(path: String, create: Boolean = true): File
    public fun read(path: String, create: Boolean = false): String
    public fun write(path: String, text: String, append: Boolean = false)
    public fun getRelativePath(file: File): String
    public fun inputStream(path: String, create: Boolean = true): InputStream
    public fun outputStream(path: String): OutputStream
    public fun getMimeType(path: String): String?
    public fun list(path: String): List<File>
}