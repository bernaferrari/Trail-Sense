# Round 9: redesign from the user's own review, in the family's language

The user reviewed every screen on their phone (dark mode). Their words are below and they win. "Ugly" means
**redesign it from scratch if needed**: one clear question per screen, M3 Expressive defaults, no clutter. The bar is
that Google could ship it to 1 billion people without complaints.

## The family language (match WiFi Weakspot, Keepout and Calcula; read their code)
- WiFi Weakspot: /Users/bernardoferrari/Documents/android/WiFiWeakspot/composeApp/src/main/kotlin/com/bernaferrari/wifiweakspot
  (feature/settings/SettingsScreen.kt for rows and choices, feature/overview for the hero, feature/analyze for charts
  and empty states).
- Keepout: /Users/bernardoferrari/Documents/android/Keepout/shared/src/commonMain/kotlin/com/bernaferrari/bravedns/ui/compose/theme
  (Rethink* components: sheets, dialogs, empty states, hero).
- Calcula: /Users/bernardoferrari/Documents/android/Calcula/shared/src/commonMain/kotlin/com/bernaferrari/calculator/ui
  (unit conversion UX, tiles, settings).
- Rules:
  - Grouped rows on surfaceContainer, 24 dp outer and 6 dp inner corners, 2 dp gaps, 72 dp, 16/12 padding.
  - A 40 dp Cookie6 icon well in 12% primary; titleMedium titles; one trailing element.
  - Connected choices in solid primary, 48 dp.
  - Buttons at ButtonDefaults.MediumContainerHeight (56 dp) with heightIn, never height(), and text never truncated.
  - One hero per screen: a display number (tnum, SemiBold or Bold) and a headlineSmall verdict.
  - Sheets on surfaceContainerLow with a titleLarge header and two equal buttons.
  - Empty states: TrailSenseEmptyState, one medium button.
  - Charts: TrailSenseChartFrame.
- The kit is already moving there (Google Sans Flex, rows, wells, choices).

## The user's notes (per screen)
- **Ruler:** a bit ugly; refine.
- **Pedometer:** ugly/bad; redesign.
- **Places/Beacons list:** the FAB sits below the nav bar (lead fixes insets in the kit).
- **Offline Maps:** the text is cut off in a button. It should be a very standard button.
- **Triangulate Location:** EXTREMELY ugly; redesign from scratch.
- **Clinometer:** super ugly; redesign from scratch.
- **Bubble Level:** ugly, confusing, super bad; redesign from scratch.
- **Clock:** nice, but the tick goes slightly beyond the dial; fix that.
- **Water Boil Timer:** great idea; keep it, polish only.
- **Tides:** rewrite it; super ugly.
- **Battery:** ugly.
- **Solar Panel:** great; keep it.
- **Light Meter:** weird and confusing.
- **Climate:** almost there; refine.
- **Temperature Estimation:** confusing.
- **Lightning Strike Distance:** the button is massive. Standardize it, though the user likes it.
- **Convert:** much harder and worse than Calcula, which has the same functionality. Port Calcula's conversion UX
  (no custom keyboard; use the system keyboard).
- **Diagnostics:** looks good. The issue text is as big as the top app bar; tone it down.
- **Turn Back:** a bit weird, and its headline is as big as the top bar.
- **Survival Guide:** the FAB sits below the nav bar (lead fixes it in the kit).
- **Signal Finder:** cute; keep it.
- **Ballistics:** nice but ugly; refine.
- **Declination:** good; can be tweaked.
- **Magnifier:** super ugly; redesign.
- **Weather:** ugly, especially when empty.
- **Astronomy:** the waning crescent moon is a bit ugly.

General: modularize, simplify, make consistent, rely on M3 Expressive defaults.

## Process
Read the style and earlier rounds only for context: trail-sense-style.md, round7.md (process rules all still apply:
compile with ANDROID_HOME, mergeDebugResources, en-US strings only, no scripts/ or guides/, never origin/main, no
push). Don't edit the shared kit (shared/src/commonMain/.../views/compose); ask the lead in your report if you need
a kit change. Keep test tags and behaviour. Commit per screen. Captures of the current state are at
/tmp/ts/p8sweep/<Name>.png (the phone, dark mode); look at them first.

Report: your branch, and one line per screen saying what you changed.
