package com.kylecorry.andromeda.permissions

fun permissionAccess(): PermissionAccess =
    MobilePermissionAccess(dev.icerock.moko.permissions.ios.PermissionsController())
