package com.kylecorry.andromeda.alerts

interface AlertPresenter {
    suspend fun confirm(title: String, message: String, confirm: String, cancel: String): Boolean
}
