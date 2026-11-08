package com.kylecorry.andromeda.print.compose

import androidx.compose.runtime.Composable
import com.kylecorry.andromeda.print.PdfPrinter
import com.kylecorry.andromeda.print.ImagePrinter

/** Remembers a system PDF printer for use by a shared Compose screen. */
@Composable
expect fun rememberPdfPrinter(): PdfPrinter

/** Remembers a system image printer for use by a shared Compose screen. */
@Composable
expect fun rememberImagePrinter(): ImagePrinter
