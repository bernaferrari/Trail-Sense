package com.kylecorry.andromeda.exceptions

import android.content.Context
import com.kylecorry.andromeda.core.system.Intents

class EmailExceptionHandler(
    context: Context,
    generator: IBugReportGenerator,
    filename: String = "errors/error.txt",
    shouldRestartApp: Boolean = true,
    shouldWrapSystemExceptionHandler: Boolean = false,
    private val messageProvider: (context: Context, log: String) -> BugReportEmailMessage,
    private val presentReport: (BugReportEmailMessage, send: () -> Unit) -> Unit,
) : BaseExceptionHandler(
    context,
    generator,
    filename,
    shouldRestartApp = shouldRestartApp,
    shouldWrapSystemExceptionHandler = shouldWrapSystemExceptionHandler
) {

    override fun handleBugReport(log: String) {
        val message = messageProvider(context, log)
        presentReport(message) {
            context.startActivity(Intents.email(message.emailAddress, message.emailSubject, log))
        }
    }
}
