package com.kylecorry.andromeda.permissions

public fun permissionAccess(): PermissionAccess =
    MobilePermissionAccess(dev.icerock.moko.permissions.ios.PermissionsController())
