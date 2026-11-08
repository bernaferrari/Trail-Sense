package com.kylecorry.andromeda.print.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.kylecorry.andromeda.print.PdfPrinter
import com.kylecorry.andromeda.print.ImagePrinter
import com.kylecorry.andromeda.print.imagePrinter
import com.kylecorry.andromeda.print.pdfPrinter

@Composable
actual fun rememberPdfPrinter(): PdfPrinter = remember { pdfPrinter() }

@Composable
actual fun rememberImagePrinter(): ImagePrinter = remember { imagePrinter() }
