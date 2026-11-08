package com.kylecorry.andromeda.permissions

import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.bluetooth.BLUETOOTH_LE
import dev.icerock.moko.permissions.camera.CAMERA
import dev.icerock.moko.permissions.contacts.READ_CONTACTS
import dev.icerock.moko.permissions.gallery.GALLERY
import dev.icerock.moko.permissions.motion.MOTION
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
import dev.icerock.moko.permissions.storage.STORAGE
import dev.icerock.moko.permissions.storage.WRITE_STORAGE
import dev.icerock.moko.permissions.location.BACKGROUND_LOCATION
import dev.icerock.moko.permissions.location.COARSE_LOCATION
import dev.icerock.moko.permissions.location.LOCATION
import dev.icerock.moko.permissions.microphone.RECORD_AUDIO

/** Adapts a platform Moko permissions controller to the shared permission API. */
fun permissionAccess(controller: PermissionsController): PermissionAccess = MobilePermissionAccess(controller)

internal class MobilePermissionAccess(private val controller: PermissionsController) : PermissionAccess {
    override suspend fun status(permission: AppPermission): PermissionStatus = when (controller.getPermissionState(permission.native())) {
        PermissionState.NotDetermined -> PermissionStatus.NotDetermined
        PermissionState.NotGranted -> PermissionStatus.NotGranted
        PermissionState.Granted -> PermissionStatus.Granted
        PermissionState.Denied -> PermissionStatus.Denied
        PermissionState.DeniedAlways -> PermissionStatus.DeniedAlways
    }

    override suspend fun request(permission: AppPermission): PermissionStatus = try {
        controller.providePermission(permission.native())
        PermissionStatus.Granted
    } catch (_: DeniedAlwaysException) {
        PermissionStatus.DeniedAlways
    } catch (_: DeniedException) {
        PermissionStatus.Denied
    }

    override fun openSettings() = controller.openAppSettings()

    private fun AppPermission.native(): Permission = when (this) {
        AppPermission.Camera -> Permission.CAMERA
        AppPermission.Location -> Permission.LOCATION
        AppPermission.CoarseLocation -> Permission.COARSE_LOCATION
        AppPermission.BackgroundLocation -> Permission.BACKGROUND_LOCATION
        AppPermission.Microphone -> Permission.RECORD_AUDIO
        AppPermission.Bluetooth -> Permission.BLUETOOTH_LE
        AppPermission.Notifications -> Permission.REMOTE_NOTIFICATION
        AppPermission.Motion -> Permission.MOTION
        AppPermission.Gallery -> Permission.GALLERY
        AppPermission.ReadContacts -> Permission.READ_CONTACTS
        AppPermission.ReadStorage -> Permission.STORAGE
        AppPermission.WriteStorage -> Permission.WRITE_STORAGE
    }
}
