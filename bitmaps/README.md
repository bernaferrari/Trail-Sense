# Multiplatform images

The `bitmaps` module provides a `PixelImage` API in common Kotlin, including image codecs and image processing. Android, iOS and JVM use their native image codecs; no Android NDK is needed for this module.

Use `PixelImage` operations for image processing and the Android conversion helpers when interoperating with a platform `Bitmap`. Channel histograms, independent channel quantization, and optional dithering are available in shared code.

`ImageRegionDecoder` opens an image from a filesystem path or encoded bytes once and decodes requested rectangular regions into `PixelImage` values. Use `samplePixel` to read or interpolate a single point and `decodeWrappedRegion` for out-of-bounds padding and horizontal wrapping. Close the decoder when finished. Android and JVM use their native region decoders; iOS crops a Core Graphics image before converting pixels. Codec support and underlying decode memory use depend on the platform and image format.
