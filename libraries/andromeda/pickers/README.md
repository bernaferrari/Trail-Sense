# Multiplatform pickers

The module exports FileKit's Compose picker APIs for files and directories on Android, iOS, and desktop. For example:

```kotlin
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher

val picker = rememberFilePickerLauncher(
    onError = { error -> /* show the error */ },
    onResult = { file -> /* null means the user cancelled */ },
)
// Call picker.launch() in response to a user action.
```

Use `rememberDirectoryPickerLauncher` for directories and FileKit's `openFileSaver` for saving. The returned `PlatformFile` handles filesystem paths and platform document URIs. The Compose launcher initializes FileKit on Android; follow [FileKit's setup instructions](https://filekit.mintlify.app/) for non-Compose use and platform permissions.
