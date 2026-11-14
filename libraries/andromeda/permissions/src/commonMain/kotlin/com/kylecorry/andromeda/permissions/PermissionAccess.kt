package com.kylecorry.andromeda.permissions

public enum class AppPermission {
    Camera, Location, CoarseLocation, BackgroundLocation, Microphone, Bluetooth,
    Notifications, Motion, Gallery, ReadContacts, ReadStorage, WriteStorage
}
public enum class PermissionStatus { NotDetermined, NotGranted, Granted, Denied, DeniedAlways }

/** The app must declare the corresponding Android permissions and iOS usage descriptions. */
public interface PermissionAccess {
    public suspend fun status(permission: AppPermission): PermissionStatus
    public suspend fun request(permission: AppPermission): PermissionStatus
    public fun openSettings()
}
