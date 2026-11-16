package com.kylecorry.andromeda.files

import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem

private val wasmFileSystem = FakeFileSystem()

internal actual fun defaultFileSystem(): FileSystem = wasmFileSystem
