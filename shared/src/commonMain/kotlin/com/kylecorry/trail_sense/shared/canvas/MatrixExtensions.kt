package com.kylecorry.trail_sense.shared.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Sets this matrix to the 2D projective transform that takes the four points in [src] to the four points in [dst]
 * (each x0, y0, x1, y1, ...), as Android's Matrix.setPolyToPoly does with four points.
 * @return false (and the identity) when the points are degenerate, such as three of them on a line
 */
fun Matrix.setPolyToPoly(src: FloatArray, dst: FloatArray): Boolean {
    require(src.size >= 8 && dst.size >= 8) { "Four points (8 values) are needed" }
    // x' = (a x + b y + c) / (g x + h y + 1), y' = (d x + e y + f) / (g x + h y + 1)
    val m = Array(8) { DoubleArray(9) }
    for (i in 0 until 4) {
        val x = src[2 * i].toDouble()
        val y = src[2 * i + 1].toDouble()
        val u = dst[2 * i].toDouble()
        val v = dst[2 * i + 1].toDouble()
        m[2 * i].let { r -> r[0] = x; r[1] = y; r[2] = 1.0; r[6] = -x * u; r[7] = -y * u; r[8] = u }
        m[2 * i + 1].let { r -> r[3] = x; r[4] = y; r[5] = 1.0; r[6] = -x * v; r[7] = -y * v; r[8] = v }
    }
    val solution = solve(m)
    reset()
    solution ?: return false
    val (a, b, c, d, e) = solution
    val f = solution[5]
    val g = solution[6]
    val h = solution[7]
    // Compose matrices transform row vectors [x, y, z, 1]
    this[0, 0] = a.toFloat()
    this[1, 0] = b.toFloat()
    this[3, 0] = c.toFloat()
    this[0, 1] = d.toFloat()
    this[1, 1] = e.toFloat()
    this[3, 1] = f.toFloat()
    this[0, 3] = g.toFloat()
    this[1, 3] = h.toFloat()
    return true
}

/** How much this matrix scales lengths along x, ignoring perspective (Android's hypot of scale x and skew y). */
val Matrix.scaleX: Float
    get() {
        val scale = this[0, 0]
        val skew = this[0, 1]
        return sqrt(scale * scale + skew * skew)
    }

/** Transforms each x, y pair in [points] in place. */
fun Matrix.mapPoints(points: FloatArray) {
    var i = 0
    while (i + 1 < points.size) {
        val mapped = map(Offset(points[i], points[i + 1]))
        points[i] = mapped.x
        points[i + 1] = mapped.y
        i += 2
    }
}

/** Gauss-Jordan elimination with partial pivoting on an n × (n + 1) augmented matrix. */
private fun solve(m: Array<DoubleArray>): DoubleArray? {
    val n = m.size
    for (col in 0 until n) {
        val pivot = (col until n).maxBy { abs(m[it][col]) }
        if (abs(m[pivot][col]) < 1e-12) return null
        val tmp = m[col]
        m[col] = m[pivot]
        m[pivot] = tmp
        val p = m[col][col]
        for (k in col..n) m[col][k] /= p
        for (row in 0 until n) {
            if (row == col) continue
            val factor = m[row][col]
            if (factor == 0.0) continue
            for (k in col..n) m[row][k] -= factor * m[col][k]
        }
    }
    return DoubleArray(n) { m[it][n] }
}
