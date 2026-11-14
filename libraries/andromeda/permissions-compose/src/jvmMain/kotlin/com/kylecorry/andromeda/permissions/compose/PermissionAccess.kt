package com.kylecorry.andromeda.permissions.compose

import androidx.compose.runtime.Composable
import com.kylecorry.andromeda.permissions.AppPermission
import com.kylecorry.andromeda.permissions.PermissionAccess
import com.kylecorry.andromeda.permissions.PermissionStatus

@Composable
public actual fun rememberPermissionAccess(): PermissionAccess = DesktopPermissionAccess

private object DesktopPermissionAccess : PermissionAccess {
    override suspend fun status(permission: AppPermission) = PermissionStatus.NotDetermined
    override suspend fun request(permission: AppPermission): PermissionStatus =
        throw UnsupportedOperationException("Permission requests are not implemented on desktop")
    override fun openSettings(): Unit = throw UnsupportedOperationException("Permission settings are not implemented on desktop")
}
