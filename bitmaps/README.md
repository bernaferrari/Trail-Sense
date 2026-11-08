# Multiplatform images

The `bitmaps` module provides a `PixelImage` API in common Kotlin, including image codecs and image processing. Android, iOS and JVM use their native image codecs; no Android NDK is needed for this module.

The old Android-only bitmap toolkit is available separately as `com.kylecorry.andromeda:bitmaps-android`. Add it explicitly if you still use its Android `Bitmap` APIs or native operations.

`ImageRegionDecoder` opens encoded image bytes once and decodes requested rectangular regions into `PixelImage` values. Use `decodeWrappedRegion` for out-of-bounds padding and horizontal wrapping. Close the decoder when finished. Android and JVM use their native region decoders; iOS crops a Core Graphics image before converting pixels. Codec support and underlying decode memory use depend on the platform and image format.
