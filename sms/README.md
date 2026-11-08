# SMS

`SmsComposer` opens the system message composer for the user to review. `sms-compose` supplies a remembered composer for shared Compose screens on Android and iOS.

`sendSms(recipient, message)` submits a message for direct sending on Android without reporting delivery status, and `incomingSms()` receives messages while collected. Check `directSmsSupported` and request Android's send or receive SMS permissions as appropriate. Receiving in the background still requires an application-owned manifest receiver. iOS and desktop do not expose direct SMS access and report it as unsupported.

Android direct SMS uses KSensor's application context, normally initialized by AndroidX Startup. The KSensor dependency contributes its boot receiver and permission to the app manifest.
