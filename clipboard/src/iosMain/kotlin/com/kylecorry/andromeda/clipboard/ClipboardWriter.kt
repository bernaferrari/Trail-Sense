package com.kylecorry.andromeda.clipboard

import platform.UIKit.UIPasteboard

public fun clipboardWriter(): ClipboardWriter = ClipboardWriter { text ->
    UIPasteboard.generalPasteboard.string = text
}
