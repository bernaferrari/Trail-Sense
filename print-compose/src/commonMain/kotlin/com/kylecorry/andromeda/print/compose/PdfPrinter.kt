package com.kylecorry.andromeda.print.compose

import androidx.compose.runtime.Composable
import com.kylecorry.andromeda.print.PdfPrinter

/** Remembers a system PDF printer for use by a shared Compose screen. */
@Composable
expect fun rememberPdfPrinter(): PdfPrinter
