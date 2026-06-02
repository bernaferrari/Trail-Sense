# Trail Sense Shared

This is a **real** Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP) module.

## Current Status

- **Real KMP module**: Uses `org.jetbrains.kotlin.multiplatform` + `org.jetbrains.compose`.
- **Targets**:
  - `androidTarget` (consumed by the main Trail Sense Android app)
  - `iosArm64` + `iosSimulatorArm64` (real iOS targets — the honest foundation)
- **Source sets**: `commonMain`, `androidMain`, `iosMain`.
- **What is actually shared today**:
  - The full Trail Sense design system (theme, tokens, `TrailSense*` components, buttons, cards, etc.).
  - `TrailSenseTheme` with proper `expect`/`actual` for Android dynamic (Material You) color support.
- **Android-only** (correctly in `androidMain`):
  - `ThemeRepository` (uses DataStore + Context for persisting color scheme choice).

## What This Enables

- Any new pure UI components or design system work can be written once in `commonMain` and will work on Android today and iOS (and future targets) tomorrow.
- Platform-specific implementations (sensors, persistence, files, dynamic color, etc.) live behind `expect`/`actual` in the appropriate `...Main` source set.

## Rules (No Fake KMP)

- Pure Kotlin + Compose UI that does not touch platform APIs → `commonMain`.
- Android-only (DataStore, `LocalContext`, sensors via Andromeda, Room, etc.) → `androidMain`.
- iOS-specific implementations → `iosMain`.
- Never put `import android.*` or `LocalContext` directly in `commonMain`.

The previous "Android library pretending to have a commonMain" setup has been removed.

## Next Steps Toward More Sharing

See the project beads issues / epics for the roadmap (domain models, repository interfaces, sensor abstractions, etc.).
