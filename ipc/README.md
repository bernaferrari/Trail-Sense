# IPC

Define shared request handlers with `IpcRouter`, and send `InterprocessCommunicationRequest` values through an `IpcClient`. A router returns `CODE_NOT_FOUND` for unknown paths and `CODE_INTERNAL_ERROR` for handler failures; coroutine cancellation is propagated. A router can also be used directly as an in-process client.

`localIpcServer(router, token, scope)` starts a loopback TCP server. Its `port` can be passed to `LocalIpcClient(port, token)` in another process. Close the server when it is no longer needed. The transport bounds request and response sizes, limits concurrent clients and supports a configurable timeout. Both processes must receive the same nonempty token through an appropriate platform mechanism.

Loopback TCP has different security and lifecycle properties from Android Messenger: it provides no operating-system caller identity or encryption, and possession of the token grants access. Android requires the `INTERNET` permission even for loopback sockets. iOS sandbox and background execution restrictions apply; cross-app communication must be verified for the intended deployment.

On Android, pass the shared router to `InterprocessCommunicationRouter` and expose it through an `InterprocessCommunicationService`. `InterprocessCommunicationClient` implements the shared `IpcClient` contract using Android Messenger. Handlers can capture application services when the router is constructed. Bind and component permissions remain the app's responsibility.
