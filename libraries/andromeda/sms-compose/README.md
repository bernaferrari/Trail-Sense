# Compose messaging

Call `rememberSmsComposer()` from shared Compose UI, then call `compose(recipient, message)` on the UI thread to open the Android or iOS message composer. The user reviews and sends the message in the system app. iOS requires a device configured to send messages; desktop currently reports the composer as unsupported.
