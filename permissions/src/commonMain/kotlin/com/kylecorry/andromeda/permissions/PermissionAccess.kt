package com.kylecorry.andromeda.permissions

enum class AppPermission { Camera, Location, CoarseLocation, BackgroundLocation, Microphone, Bluetooth }
enum class PermissionStatus { NotDetermined, NotGranted, Granted, Denied, DeniedAlways }

/** The app must declare the corresponding Android permissions and iOS usage descriptions. */
interface PermissionAccess {
    suspend fun status(permission: AppPermission): PermissionStatus
    suspend fun request(permission: AppPermission): PermissionStatus
    fun openSettings()
}
