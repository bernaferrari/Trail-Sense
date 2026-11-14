# Multiplatform files

`FileStorage` uses Okio for regular filesystem paths. The module also exports FileKit's `PlatformFile` APIs for files selected by the user, including Android document URIs and Apple security-scoped resources. Use `PlatformFile.readBytes()` and FileKit's file operations instead of treating a document URI as a filesystem path. FileKit's Compose picker launchers are available from the `pickers` module.

ZIP operations on `FileStorage` can create, extract, list, and read entries, including password-protected archives. Creating an archive inside a source directory is rejected to prevent the archive from including itself.
