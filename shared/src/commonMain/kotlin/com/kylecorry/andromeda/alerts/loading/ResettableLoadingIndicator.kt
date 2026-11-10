package com.kylecorry.andromeda.alerts.loading

public class ResettableLoadingIndicator(private val loadingIndicator: LoadingIndicator) :
    LoadingIndicator {

    private var hasRun = false

    override fun show() {
        if (!hasRun) {
            loadingIndicator.show()
            hasRun = true
        }
    }

    override fun hide() {
        loadingIndicator.hide()
    }

    public fun reset() {
        hasRun = false
    }
}