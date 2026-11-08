# Compose permissions

Call `rememberPermissionAccess()` from shared Compose UI to obtain a lifecycle-bound permission controller on Android and iOS. Use its suspending `status` and `request` methods from a coroutine, and declare the relevant Android permissions and iOS usage descriptions in the host app. On Android the composition must have a `ComponentActivity` context.

Desktop permission requests and settings are not implemented; status is reported as `NotDetermined` and attempting a request or opening settings throws `UnsupportedOperationException`.
