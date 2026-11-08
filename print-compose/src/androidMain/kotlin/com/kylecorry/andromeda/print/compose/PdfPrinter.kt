package com.kylecorry.andromeda.print.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.andromeda.print.PdfPrinter
import com.kylecorry.andromeda.print.pdfPrinter

@Composable
actual fun rememberPdfPrinter(): PdfPrinter {
    val context = LocalContext.current
    val activity = checkNotNull(context.findActivity()) { "Printing requires an Activity context" }
    return remember(activity) { pdfPrinter(activity) }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> if (baseContext === this) null else baseContext.findActivity()
    else -> null
}
