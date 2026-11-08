package com.kylecorry.andromeda.files

import okio.FileSystem

internal actual fun defaultFileSystem(): FileSystem = FileSystem.SYSTEM
