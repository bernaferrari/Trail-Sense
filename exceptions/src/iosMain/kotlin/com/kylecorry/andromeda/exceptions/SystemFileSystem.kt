package com.kylecorry.andromeda.exceptions

import okio.FileSystem

internal actual fun defaultFileSystem(): FileSystem = FileSystem.SYSTEM
