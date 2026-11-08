package com.kylecorry.andromeda.sms

/** Opens the system message composer with a recipient and text for the user to review. */
fun interface SmsComposer {
    fun compose(recipient: String, message: String)
}
