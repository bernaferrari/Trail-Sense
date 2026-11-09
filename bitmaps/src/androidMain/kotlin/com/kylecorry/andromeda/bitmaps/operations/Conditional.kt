package com.kylecorry.andromeda.bitmaps.operations

import android.graphics.Bitmap

public class Conditional : BitmapOperation {

    private val predicate: (Bitmap) -> Boolean
    private val operation: BitmapOperation
    private val falseOperation: BitmapOperation

    public constructor(
        shouldRun: Boolean,
        operation: BitmapOperation,
        falseOperation: BitmapOperation = NoOp()
    ) {
        predicate = { shouldRun }
        this.operation = operation
        this.falseOperation = falseOperation
    }

    public constructor(
        operation: BitmapOperation,
        predicate: (Bitmap) -> Boolean,
        falseOperation: BitmapOperation = NoOp()
    ) {
        this.predicate = predicate
        this.operation = operation
        this.falseOperation = falseOperation
    }

    override fun execute(bitmap: Bitmap): Bitmap {
        return if (predicate(bitmap)) {
            operation.execute(bitmap)
        } else {
            falseOperation.execute(bitmap)
        }
    }
}