package com.kylecorry.andromeda.permissions.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.kylecorry.andromeda.permissions.PermissionAccess
import com.kylecorry.andromeda.permissions.permissionAccess
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory

@Composable
actual fun rememberPermissionAccess(): PermissionAccess {
    val factory = rememberPermissionsControllerFactory()
    val controller = remember(factory) { factory.createPermissionsController() }
    BindEffect(controller)
    return remember(controller) { permissionAccess(controller) }
}
