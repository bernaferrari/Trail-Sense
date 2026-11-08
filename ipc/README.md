# IPC

Define shared request handlers with `IpcRouter`, and send `InterprocessCommunicationRequest` values through an `IpcClient`. A router returns `CODE_NOT_FOUND` for unknown paths and `CODE_INTERNAL_ERROR` for handler failures; coroutine cancellation is propagated. A router can also be used directly as an in-process client.

On Android, pass the shared router to `InterprocessCommunicationRouter` and expose it through an `InterprocessCommunicationService`. `InterprocessCommunicationClient` implements the shared `IpcClient` contract using Android Messenger. Handlers can capture application services when the router is constructed. Bind and component permissions remain the app's responsibility.

The shared router is transport-independent. iOS and JVM currently have no interprocess transport in this module; using the router directly stays in the same process.
