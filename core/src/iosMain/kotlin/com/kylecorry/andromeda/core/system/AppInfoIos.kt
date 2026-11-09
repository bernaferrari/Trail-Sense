package com.kylecorry.andromeda.core.system

import platform.Foundation.NSBundle

actual fun appInfo(): AppInfo {
    val bundle = NSBundle.mainBundle
    return AppInfo(bundle.bundleIdentifier, bundle.objectForInfoDictionaryKey("CFBundleName") as? String,
        bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String,
        bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String)
}
