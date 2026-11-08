# Compose PDF printing

Call `rememberPdfPrinter()` from a shared Compose screen, then invoke `print(pdfBytes, jobName)` on the UI thread to open the platform print dialog. Android requires an activity context. On iPad, the underlying printer presents from the application window. Availability and print-dialog behavior depend on the platform.

`rememberImagePrinter()` similarly prints a shared `PixelImage`, fitting it to the printable page.
