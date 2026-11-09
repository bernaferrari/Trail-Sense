package com.kylecorry.andromeda.core.ui

import kotlinx.coroutines.flow.Flow

interface ProgressReporter {
    val progress: Flow<Progress>
}

data class Progress(val processed: Int, val total: Int, val message: String? = null) {
    val percent: Float get() = if (total > 0) processed.toFloat() / total else 0f
}
