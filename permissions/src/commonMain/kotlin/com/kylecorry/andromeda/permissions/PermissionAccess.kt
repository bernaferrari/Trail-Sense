package com.kylecorry.andromeda.permissions

enum class AppPermission { Camera, Location, CoarseLocation, BackgroundLocation, Microphone, Bluetooth }
enum class PermissionStatus { NotDetermined, NotGranted, Granted, Denied, DeniedAlways }

interface PermissionAccess {
    suspend fun status(permission: AppPermission): PermissionStatus
    suspend fun request(permission: AppPermission): PermissionStatus
    fun openSettings()
}
