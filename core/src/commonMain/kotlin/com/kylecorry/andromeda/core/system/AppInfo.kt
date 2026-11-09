package com.kylecorry.andromeda.core.system

/** Host application identity and version, when supplied by the platform. */
data class AppInfo(val id: String?, val name: String?, val version: String?, val build: String?)

/** Android requires KSensor context initialization; JVM launchers do not provide app metadata. */
expect fun appInfo(): AppInfo
