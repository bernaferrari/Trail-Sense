package com.kylecorry.andromeda.alerts

public interface AlertPresenter {
    public suspend fun confirm(title: String, message: String, confirm: String, cancel: String): Boolean
}
