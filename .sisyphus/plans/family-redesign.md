# Trail Sense: the family redesign

Trail Sense is being brought up to the standard of the owner's other rebuilt apps (Calcula, Keepout, Wordkeep,
SDK Monitor, ChangeDetection, WiFi Weakspot, QuietGuard): calm, consistent, polished, delightful in every screen.
This file is the brief every screen follows. Read it whole before changing a screen.

## The shared kit (use it; don't hand-roll)

All in `shared/src/commonMain/kotlin/com/kylecorry/trail_sense/shared/views/compose/`:

- `TrailSensePage(title, onBack, subtitle?, actions, floatingActionButton, state) { lazy content }`: the standard
  scrolling page. Large title that collapses into the top bar, back arrow, 16 dp gutters, centered at 720 dp on
  tablets, 16 dp between sections. Every settings page and every list/form screen that isn't a full-screen
  instrument uses it. Back: `requireActivity().onBackPressedDispatcher.onBackPressed()` (host) or the screen's
  existing back/navigateUp callback.
- `trailSenseSection(key, title?, footer?) { row { position -> ... } }`: one section in a lazy list: an optional
  header (primary titleSmall) and its rows as one grouped object: 24 dp outer corners, 6 dp inner, 2 dp gaps,
  `surfaceContainerLow`. Rows get their `position` from the rows actually shown, so conditional rows are fine.
  Outside a lazy list: `TrailSenseRowGroup(title) { row { ... } }`.
- Rows that take a `position`: `TrailSenseListItem` (title, subtitle, icon, trailing, onClick, onLongClick),
  `TrailSenseSwitchRow`, the settings rows in `app/.../settings/ui/SettingsCompose.kt` (`SettingsSwitchRow`,
  `SettingsActionRow`, `SettingsPickerRow`, `SettingsInfoRow`), and `TrailSenseGroupCard(position) { anything }` for
  a chart, preview or custom control inside a group.
- `TrailSenseConnectedChoice(options = listOf(TrailSenseChoice(value, label, icon)), selected, onSelect)`: the
  family segmented control (chosen = solid primary with a check). Use it instead of radio rows or spinners when there
  are 2 to 4 short options that fit on one line.
- `TrailSenseEmptyState(icon, title, description?, action?)`: cookie-shaped icon well, title, one calm sentence, an
  optional call to action. Never show it before the first load finishes (show nothing, or a `LoadingIndicator`).
- `TrailSenseHeroCard`, `TrailSenseMetric` (tabular numbers), `TrailSenseIconButton`, `TrailSenseCard`.
- Tokens: `TrailSenseSpacing` (4/8/12/16/24/32/48), `TrailSenseShape` (groupOuter 24, groupInner 6),
  `TrailSenseSize` (48 dp touch target, 720 dp max width).
- Dialogs: `com.kylecorry.trail_sense.shared.dialogs.Alerts / Pickers / CoroutineAlerts / CoroutinePickers` are
  already Compose (same API as Andromeda). Inside Compose code you may instead use `AlertDialog` / `ModalBottomSheet`
  with 28 dp corners and `surfaceContainerHigh`/`surfaceContainerLow`.
- Reference screens already done: `app/.../settings/ui/ThemeSettingsScreen.kt` (a settings page) and
  `app/.../tools/ToolsScreen.kt` (the Tools tab).

## The rules

1. **One structure per screen type.** Settings and lists: `TrailSensePage` + sections. Instruments (compass, level,
   clinometer, ruler, flashlight, whistle…): full-bleed, one big hero reading in `TrailSenseMetric`/display type,
   controls grouped below or in a bottom bar, the same top bar with a back arrow when the screen is not a tab.
2. **Groups, not dividers.** Rows of a group touch with 2 dp gaps and the 24/6 silhouette. Remove
   `TrailSenseDivider()` between rows and any extra `padding(horizontal = …)` added around rows (the page owns the
   gutters). No borders, no outlines for emphasis: emphasis is a tone change.
3. **Calm surfaces.** Screen `surface`; rows and cards `surfaceContainerLow`; sheets/dialogs `surfaceContainerHigh`;
   unselected segments `surfaceContainerHighest`. Accent color for the one thing that matters on the screen.
4. **Type.** Screen titles come from the page. Section headers: `TrailSenseGroupHeader` (primary titleSmall). Row
   titles bodyLarge, supporting text bodyMedium onSurfaceVariant. Live readings: displayMedium/displaySmall,
   tabular figures (`fontFeatureSettings = "tnum"`), units in titleMedium onSurfaceVariant beside them.
5. **Say it once, say it plainly.** Sentence case. No subtitle that repeats the title, no count that repeats the
   header. Labels say what a choice does. Confirm buttons name the action ("Delete beacon", not "OK") when you are
   writing new strings. Use `·` as a separator.
6. **Only show choices that work.** Hide a row that can't apply (missing sensor, depends on another switch that's
   off) rather than showing it disabled.
7. **Motion.** `MaterialTheme.motionScheme` specs (fastSpatialSpec / defaultEffectsSpec), `AnimatedVisibility`
   with expandVertically/shrinkVertically for rows appearing, `animateContentSize` for cards changing size, press
   scale 0.96 to 0.985 on tiles and rows. Nothing bounces for no reason; nothing animates on every recomposition.
8. **Touch targets.** 48 dp minimum. Icon buttons always have a content description.
9. **Loading and empty.** Never flash "No data"/"Empty" before the first load. Keep layout stable while values
   arrive (hold space, show `--` or a quiet placeholder).
10. **Behavior stays.** This is a visual and UX redesign: keep every feature, preference key, navigation action and
    data flow. Don't change domain or infrastructure code unless a screen truly needs it.

## Strings

- New strings go in `app/src/main/res/values/strings.xml` only (English). Never edit other locales, the guides, or
  `guides/` (see AGENTS.md/CLAUDE.md).
- Prefer reusing existing strings; grep before adding.

## Build and check

- `./gradlew assembleDebug -q` must pass (copy `local.properties` from the main checkout into a worktree first).
- `scripts/unit-tests.sh <filter>` for anything with logic you touched.
- The only emulator (emulator-5578) is reserved for the coordinating session: don't install or drive it.
- Compose previews are welcome for new composables.
