package com.kylecorry.andromeda.bitmaps

/** YUV 4:2:0 formats: NV21 interleaves V/U; YV12 stores aligned V and U planes. */
enum class YuvFormat(val value: Int) {
    NV21(0x11),
    YV12(0x32315659),
}
