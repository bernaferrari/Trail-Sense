package com.kylecorry.andromeda.clipboard

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

public fun clipboardWriter(): ClipboardWriter = ClipboardWriter { text ->
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
}
