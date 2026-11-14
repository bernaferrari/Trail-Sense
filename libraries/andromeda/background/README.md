# Background work

`BackgroundScheduler` requests one platform-managed run no earlier than a given instant. `PeriodicBackgroundScheduler` requests recurring work with an interval of at least 15 minutes. Exact timing is controlled by the operating system; neither API guarantees execution at a particular time.

On Android, create either scheduler with a context, unique identifier, and WorkManager worker class. WorkManager reschedules periodic work automatically. On iOS, register the identifier with `BGTaskScheduler` at app launch, include it in the app's permitted background task identifiers, and enable the required background mode. For periodic work, recreate the scheduler and call `onTaskStarted()` from the registered task handler before doing the work; this schedules the next run using the interval persisted by the scheduler. Complete the platform task using its completion API. Use distinct identifiers for one-shot and periodic work.

`CoroutineTaskScheduler` is available on all targets for tasks that only need to run while their coroutine scope remains alive.
