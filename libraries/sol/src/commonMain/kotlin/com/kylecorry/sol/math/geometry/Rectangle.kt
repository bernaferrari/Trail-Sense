package com.kylecorry.sol.math.geometry

import com.kylecorry.sol.math.Vector2

public data class Rectangle(val left: Float, val top: Float, val right: Float, val bottom: Float) {

    val center: Vector2 = Vector2((left + right) / 2, (top + bottom) / 2)
    val topLeft: Vector2 = Vector2(left, top)
    val topRight: Vector2 = Vector2(right, top)
    val bottomLeft: Vector2 = Vector2(left, bottom)
    val bottomRight: Vector2 = Vector2(right, bottom)

    val corners: List<Vector2> = listOf(topLeft, topRight, bottomLeft, bottomRight)

    public fun width(): Float {
        return right - left
    }

    public fun height(): Float {
        return top - bottom
    }

    public fun area(): Float {
        return width() * height()
    }

    public fun contains(point: Vector2): Boolean {
        return point.x in left..right && point.y in bottom..top
    }

    public fun contains(rectangle: Rectangle): Boolean {
        return contains(rectangle.topLeft) &&
                contains(rectangle.topRight) &&
                contains(rectangle.bottomLeft) &&
                contains(rectangle.bottomRight)
    }

    public fun intersects(rectangle: Rectangle): Boolean {
        return left <= rectangle.right &&
                right >= rectangle.left &&
                bottom <= rectangle.top &&
                top >= rectangle.bottom
    }

    public fun rotate(angle: Float, center: Vector2 = this.center): Rectangle {
        val rotated = corners.map { it.rotate(angle, center) }
        return boundingBox(rotated)
    }

    public companion object {
        public fun fromCenter(center: Vector2, width: Float, height: Float): Rectangle {
            val halfWidth = width / 2
            val halfHeight = height / 2
            return Rectangle(
                center.x - halfWidth,
                center.y + halfHeight,
                center.x + halfWidth,
                center.y - halfHeight
            )
        }

        public fun fromCorners(topLeft: Vector2, bottomRight: Vector2): Rectangle {
            return Rectangle(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
        }

        public fun fromCorners(topLeft: Vector2, width: Float, height: Float): Rectangle {
            return Rectangle(topLeft.x, topLeft.y, topLeft.x + width, topLeft.y - height)
        }

        public fun boundingBox(points: List<Vector2>): Rectangle {
            if (points.isEmpty()) {
                return Rectangle(0f, 0f, 0f, 0f)
            }

            val x = points.map { it.x }
            val y = points.map { it.y }
            return Rectangle(x.min(), y.max(), x.max(), y.min())
        }
    }
}
