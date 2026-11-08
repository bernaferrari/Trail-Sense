package com.kylecorry.andromeda.clipboard

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

fun clipboardWriter(): ClipboardWriter = ClipboardWriter { text ->
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
}
