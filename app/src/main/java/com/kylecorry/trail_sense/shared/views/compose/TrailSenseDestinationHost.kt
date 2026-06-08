package com.kylecorry.trail_sense.shared.views.compose

import android.content.Context
import android.net.Uri
import android.util.Size
import androidx.annotation.StringRes
import androidx.navigation.NavController
import com.kylecorry.trail_sense.shared.sharing.ActionItem

interface TrailSenseDestinationHost {
    fun requireContext(): Context
    fun getString(@StringRes resId: Int): String
    fun getString(@StringRes resId: Int, vararg formatArgs: Any?): String
    fun findNavController(): NavController
    fun requestCamera(action: (hasPermission: Boolean) -> Unit)
    fun showPhotoImport(
        resolution: Size?,
        useZeroShutterLag: Boolean,
        onCapture: (Uri?) -> Unit
    )

    fun showActionSheet(
        title: String,
        subtitle: String?,
        actions: List<ActionItem>,
        onAction: (ActionItem?) -> Unit
    )
}
