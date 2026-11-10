package com.kylecorry.andromeda.core.ui

import kotlinx.coroutines.flow.Flow

public interface ProgressReporter {
    public val progress: Flow<Progress>
}

public data class Progress(val processed: Int, val total: Int, val message: String? = null) {
    val percent: Float get() = if (total > 0) processed.toFloat() / total else 0f
}
