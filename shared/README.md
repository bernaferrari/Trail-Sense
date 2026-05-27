# Trail Sense Shared

This module contains shared code intended for future Kotlin Multiplatform use.

## Current Status (as of 2026)

- **Not yet a full KMP module**: Still compiled as an `com.android.library` with the Compose compiler.
- **Source set layout**: `commonMain` (intended for future shared logic + UI) + `androidMain` (Android-specific, e.g. ThemeRepository with DataStore).
- **What is shared today**: The Trail Sense design system (`TrailSenseTheme`, design tokens, `TrailSense*` components) and some utilities. These are the foundation for the modern Compose experience.
- **Goal**: Move pure Kotlin domain logic, math, models, and eventually more UI into `commonMain` so the app can target additional platforms when the Android modernization is further along.

## Migration Guidance

- Prefer putting new pure Kotlin code in `commonMain/kotlin`.
- Android-only code (sensors, UI that needs Context in special ways, DataStore, etc.) belongs in `androidMain`.
- The design system components currently live in commonMain and work on Android. They are written to be portable.

See the main project epic `trail-sense-kmp-00q` in beads for the overall Compose + KMP roadmap.
