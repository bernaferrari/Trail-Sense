# Multiplatform images

The `bitmaps` module provides a `PixelImage` API in common Kotlin, including image codecs and image processing. Android, iOS and JVM use their native image codecs; no Android NDK is needed for this module.

The old Android-only bitmap toolkit is available separately as `com.kylecorry.andromeda:bitmaps-android`. Add it explicitly if you still use its Android `Bitmap` APIs or native operations.
