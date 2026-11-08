package com.kylecorry.andromeda.permissions

import androidx.activity.ComponentActivity
import dev.icerock.moko.permissions.PermissionsController

fun permissionAccess(activity: ComponentActivity): PermissionAccess {
    val controller = PermissionsController(activity.applicationContext)
    controller.bind(activity)
    return MobilePermissionAccess(controller)
}
