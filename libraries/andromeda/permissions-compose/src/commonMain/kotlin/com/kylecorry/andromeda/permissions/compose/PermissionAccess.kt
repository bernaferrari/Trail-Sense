package com.kylecorry.andromeda.permissions.compose

import androidx.compose.runtime.Composable
import com.kylecorry.andromeda.permissions.PermissionAccess

/** Remembers a permission controller bound to the current screen's lifecycle. */
@Composable
public expect fun rememberPermissionAccess(): PermissionAccess
