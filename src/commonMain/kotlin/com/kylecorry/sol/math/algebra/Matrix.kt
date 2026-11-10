package com.kylecorry.sol.math.algebra

import kotlin.jvm.JvmInline

import com.kylecorry.sol.math.Vector
import kotlin.math.sqrt

@JvmInline
public value class Matrix internal constructor(private val rawData: FloatArray) {

    public operator fun get(row: Int, column: Int): Float {
        return rawData[getIndex(row, column)]
    }

    public operator fun set(row: Int, column: Int, value: Float) {
        rawData[getIndex(row, column)] = value
    }

    public fun sum(): Float {
        var sum = 0.0
        for (i in 2..rawData.lastIndex) {
            sum += rawData[i]
        }
        return sum.toFloat()
    }

    public fun max(): Float {
        if (rawData.size <= 2) {
            return 0f
        }
        var max = rawData[2]
        for (i in 3..rawData.lastIndex) {
            max = maxOf(max, rawData[i])
        }
        return max
    }

    public fun norm(): Float {
        var sum = 0.0
        for (i in 2..rawData.lastIndex) {
            sum += rawData[i] * rawData[i].toDouble()
        }
        check(sum >= 0) { "Sum of squares must be non-negative but was $sum" }
        return sqrt(sum.toFloat())
    }

    public fun rows(): Int {
        return rawData[0].toRawBits()
    }

    public fun columns(): Int {
        return rawData[1].toRawBits()
    }

    public fun size(): Int {
        return rows() * columns()
    }

    public fun clone(): Matrix {
        return Matrix(rawData.copyOf())
    }

    public fun setRow(row: Int, rowData: FloatArray) {
        require(rowData.size == columns()) {
            "Expected row to be of length ${columns()} but got ${rowData.size}"
        }
        val startIndex = getIndex(row, 0)
        rowData.copyInto(rawData, startIndex)
    }

    public fun getRow(row: Int, destination: FloatArray = FloatArray(columns())): FloatArray {
        require(destination.size == columns()) {
            "Expected destination to be of length ${columns()} but got ${destination.size}"
        }
        val startIndex = getIndex(row, 0)
        rawData.copyInto(destination, startIndex = startIndex, endIndex = startIndex + columns())
        return destination
    }

    public fun setColumn(column: Int, columnData: FloatArray) {
        require(columnData.size == rows()) {
            "Expected column to be of length ${rows()} but got ${columnData.size}"
        }
        for (row in 0..<rows()) {
            set(row, column, columnData[row])
        }
    }

    public fun getColumn(column: Int, destination: FloatArray = FloatArray(rows())): FloatArray {
        require(destination.size == rows()) {
            "Expected destination to be of length ${rows()} but got ${destination.size}"
        }
        for (row in 0..<rows()) {
            destination[row] = get(row, column)
        }
        return destination
    }

    public fun swapRows(row1: Int, row2: Int) {
        val temp = getRow(row1)
        setRow(row1, getRow(row2))
        setRow(row2, temp)
    }

    public fun toVector(): Vector {
        require(rows() == 1 || columns() == 1) { "Expected the matrix to be a row or column matrix" }
        return Vector(if (rows() == 1) getRow(0) else getColumn(0))
    }

    private fun getIndex(row: Int, column: Int): Int {
        require(row in 0..<rows()) {
            "Expected row to be between 0 and ${rows()} but got $row"
        }
        require(column in 0..<columns()) {
            "Expected column to be between 0 and ${columns()} but got $column"
        }
        return 2 + columns() * row + column
    }

    public companion object {
        public fun zeros(rows: Int, columns: Int): Matrix {
            require(rows >= 0) { "rows must be non-negative, but was $rows" }
            require(columns >= 0) { "columns must be non-negative, but was $columns" }
            val data = FloatArray(rows * columns + 2)
            data[0] = Float.fromBits(rows)
            data[1] = Float.fromBits(columns)
            val matrix = Matrix(data)
            check(matrix.rows() == rows) { "The matrix doesn't have the right number of rows" }
            check(matrix.columns() == columns) { "The matrix doesn't have the right number of columns" }
            return matrix
        }

        public fun create(rows: Int, columns: Int, value: Float = 0f): Matrix {
            require(rows >= 0) { "rows must be non-negative, but was $rows" }
            require(columns >= 0) { "columns must be non-negative, but was $columns" }
            val data = FloatArray(rows * columns + 2) { value }
            data[0] = Float.fromBits(rows)
            data[1] = Float.fromBits(columns)
            val matrix = Matrix(data)
            check(matrix.rows() == rows) { "The matrix doesn't have the right number of rows" }
            check(matrix.columns() == columns) { "The matrix doesn't have the right number of columns" }
            return matrix
        }

        public fun createFromRawData(data: FloatArray): Matrix {
            return Matrix(data)
        }

        public fun create(rows: Int, columns: Int, data: FloatArray): Matrix {
            require(rows >= 0) { "rows must be non-negative, but was $rows" }
            require(columns >= 0) { "columns must be non-negative, but was $columns" }
            require(data.size == rows * columns) {
                "Expected data to be of length ${rows * columns} but got ${data.size}"
            }
            val newData = FloatArray(2 + data.size)
            newData[0] = Float.fromBits(rows)
            newData[1] = Float.fromBits(columns)
            data.copyInto(newData, 2)
            val matrix = Matrix(newData)
            check(matrix.rows() == rows) { "The matrix doesn't have the right number of rows" }
            check(matrix.columns() == columns) { "The matrix doesn't have the right number of columns" }
            return matrix
        }

        public fun identity(size: Int): Matrix {
            require(size >= 0) { "size must be non-negative, but was $size" }
            val matrix = diagonal(values = FloatArray(size) { 1f })
            check(matrix.rows() == size) { "The matrix doesn't have the right number of rows" }
            check(matrix.columns() == size) { "The matrix doesn't have the right number of columns" }
            return matrix
        }

        public fun diagonal(vararg values: Float): Matrix {
            val matrix = create(values.size, values.size) { r, c -> if (r == c) values[r] else 0f }
            check(matrix.rows() == values.size) { "The matrix doesn't have the right number of rows" }
            check(matrix.columns() == values.size) { "The matrix doesn't have the right number of columns" }
            return matrix
        }

        public fun column(vararg values: Float): Matrix {
            val matrix = create(values.size, 1) { r, _ -> values[r] }
            check(matrix.rows() == values.size) { "The matrix doesn't have the right number of rows" }
            check(matrix.columns() == 1) { "The matrix doesn't have the right number of columns" }
            return matrix
        }

        public fun row(vararg values: Float): Matrix {
            val matrix = create(1, values.size) { _, c -> values[c] }
            check(matrix.rows() == 1) { "The matrix doesn't have the right number of rows" }
            check(matrix.columns() == values.size) { "The matrix doesn't have the right number of columns" }
            return matrix
        }

        public fun create(oldMatrix: Array<FloatArray>): Matrix {
            val rows = oldMatrix.size
            val columns = oldMatrix.getOrNull(0)?.size ?: 0
            val matrix = create(rows, columns) { r, c ->
                oldMatrix[r][c]
            }
            check(matrix.rows() == rows) { "The matrix doesn't have the right number of rows" }
            check(matrix.columns() == columns) { "The matrix doesn't have the right number of columns" }
            return matrix
        }

        public inline fun create(
            rows: Int,
            columns: Int,
            crossinline initialize: (row: Int, column: Int) -> Float
        ): Matrix {
            require(rows >= 0) { "rows must be non-negative, but was $rows" }
            require(columns >= 0) { "columns must be non-negative, but was $columns" }
            val newData = FloatArray(2 + rows * columns)
            newData[0] = Float.fromBits(rows)
            newData[1] = Float.fromBits(columns)
            for (i in 2..<newData.size) {
                val index = i - 2
                val row = index / columns
                val column = index % columns
                require(row <= rows) { "Row $row out of range $rows" }
                require(column <= columns) { "Column $column out of range $columns" }
                newData[i] = initialize(row, column)
            }
            val matrix = createFromRawData(newData)
            check(matrix.rows() == rows) { "The matrix doesn't have the right number of rows" }
            check(matrix.columns() == columns) { "The matrix doesn't have the right number of columns" }
            return matrix
        }
    }
}


public fun Matrix.dot(other: Matrix): Matrix {
    return LinearAlgebra.dot(this, other)
}

public fun Matrix.dot(other: Vector): Matrix {
    return LinearAlgebra.dot(this, other)
}

public fun Matrix.add(other: Matrix): Matrix {
    return LinearAlgebra.add(this, other)
}

public fun Matrix.add(value: Float): Matrix {
    return LinearAlgebra.add(this, value)
}

public fun Matrix.subtract(other: Matrix): Matrix {
    return LinearAlgebra.subtract(this, other)
}

public fun Matrix.subtract(value: Float): Matrix {
    return LinearAlgebra.subtract(this, value)
}

public fun Matrix.multiply(other: Matrix): Matrix {
    return LinearAlgebra.multiply(this, other)
}

public fun Matrix.multiply(scale: Float): Matrix {
    return LinearAlgebra.multiply(this, scale)
}

public fun Matrix.divide(other: Matrix): Matrix {
    return LinearAlgebra.divide(this, other)
}

public fun Matrix.divide(scale: Float): Matrix {
    return LinearAlgebra.divide(this, scale)
}

public fun Matrix.transpose(): Matrix {
    return LinearAlgebra.transpose(this)
}

public fun Matrix.mapped(fn: (Float) -> Float): Matrix {
    return LinearAlgebra.map(this, fn)
}

public fun Matrix.mapRows(fn: (FloatArray) -> FloatArray): Matrix {
    return LinearAlgebra.mapRows(this, fn)
}

public fun Matrix.mapColumns(fn: (FloatArray) -> FloatArray): Matrix {
    return LinearAlgebra.mapColumns(this, fn)
}

public inline fun Matrix.forEach(crossinline fn: (value: Float) -> Unit) {
    for (row in 0..<rows()) {
        for (column in 0..<columns()) {
            fn(get(row, column))
        }
    }
}

public fun Matrix.sumRows(): Matrix {
    return LinearAlgebra.sumRows(this)
}

public fun Matrix.sumColumns(): Matrix {
    return LinearAlgebra.sumColumns(this)
}

public fun Matrix.maxRows(): Matrix {
    return LinearAlgebra.maxRows(this)
}

public fun Matrix.maxColumns(): Matrix {
    return LinearAlgebra.maxColumns(this)
}

public fun Matrix.inverse(): Matrix {
    return LinearAlgebra.inverse(this)
}

public fun Matrix.adjugate(): Matrix {
    return LinearAlgebra.adjugate(this)
}

public fun Matrix.det(): Float {
    return LinearAlgebra.determinant(this)
}

public fun Matrix.cofactor(r: Int, c: Int): Matrix {
    return LinearAlgebra.cofactor(this, r, c)
}

public fun Matrix.appendColumn(col: FloatArray): Matrix {
    return LinearAlgebra.appendColumn(this, col)
}

public fun Matrix.appendColumn(value: Float): Matrix {
    return LinearAlgebra.appendColumn(this, value)
}

public fun Matrix.appendRow(row: FloatArray): Matrix {
    return LinearAlgebra.appendRow(this, row)
}

public fun Matrix.appendRow(value: Float): Matrix {
    return LinearAlgebra.appendRow(this, value)
}
