# Expressive pass: every screen to 9/10

The redesign is "fine but generic" (reviewer scores 4–7). The family apps (Keepout, SDKMonitor, WiFi Weakspot,
QuietGuard, Calcula) score 9 because every screen has **one saturated, expressive focal point**, values that carry
**meaning through colour**, and nothing that looks default or legacy. Reference captures: /tmp/ts/ref/*.png — look
at them before you start. Baseline captures of Trail Sense: /tmp/ts/base/<Tool>.png. Reviewer brief:
/tmp/ts/review_brief.md. First-round reviews: /tmp/ts/review1.md.

## The kit (shared/src/commonMain/.../shared/views/compose/)
- `TrailSenseSpotlight(title, eyebrow, supportingText, tone, badge, trailing, actions, titleStyle) { content }` — the
  hero. One per screen at most. Saturated tone container (Primary/Secondary/Tertiary/Good/Caution/Error), a badge
  on the left, SemiBold tabular title, connected pill actions inside it. Model: Keepout "Protected" card.
- `TrailSenseShapeBadge(text | icon, shape = MaterialShapes.Cookie9Sided.toShape(), size, containerColor, contentColor, iconRotation)`
  — a value or icon inside an expressive shape (Cookie4/6/7/9/12Sided, Sunny, VerySunny, Flower, Clover4Leaf,
  Pentagon, Gem, Burst, SoftBurst, Pill...). Use for status, counts, steps, list leading items.
- `TrailSenseStatGrid(stats: List<TrailSenseStat(label, value, supportingText, tone, icon, onClick)>, columns)` —
  connected value tiles (SDKMonitor's Target/Min SDK, Calcula's statistics). Toned tiles stand out.
- `trailSenseToneColors(tone)`, `trailSenseStatusColors()` for good/caution semantics.
- Existing: `TrailSensePage` (lazy page; medium bar for sub-pages), `trailSenseSection { row { position -> } }`,
  `TrailSenseListItem`, `TrailSenseInputField` (filled, unit chip), `TrailSenseConnectedChoice`, `TrailSenseGroupCard`,
  `TrailSenseEmptyState`, `TrailSenseReading`, `TrailSenseMetricRow`, `TrailSenseTileGrid`, `TrailSenseIconWell`.
- Icons: `TrailIcons.X` vectors (`import com.kylecorry.trail_sense.shared.icons.trail.*`); add new ones as new files
  in shared/.../icons/trail/ if needed. No drawables, ever.
- Motion: `MaterialTheme.motionScheme.*Spec()`; `LinearWavyProgressIndicator`/`CircularWavyProgressIndicator` for progress.

## Rules of taste
- One focal point per screen, saturated (Spotlight, a shaped dial, a toned stat grid). Everything else quiet and grouped.
- Big numbers SemiBold + tnum. Never Light/Thin weights for readings.
- Colour means something: good/caution/error tones for states, tertiary for warm/high, primary for cool/low.
- No legacy look: no OutlinedTextField, no black/grey placeholder boxes, no thin 1px rings, no hard-coded AppColor
  red/blue in charts (use scheme colours, filled areas, dashed light grids, rounded bars).
- Groups: 24 dp outer / 6 dp inner, 2 dp gaps, surfaceContainerLow; no dividers or borders. Sentence case; "·" separator.
- Hide what can't apply rather than disabling it. Empty states: big expressive shape, warm copy, one clear action.
- Keep behaviour, test tags and accessibility semantics. Don't remove features.

## Process for each screen
1. Read its baseline capture and its review (or review it yourself against the brief).
2. Redesign it with the kit; push past the review's fixes — be creative (the "bold" idea is welcome when tasteful).
3. `./gradlew :app:compileDebugKotlin -q` must pass. Commit per screen or small group in your worktree.

## Boundaries
- Only edit files under your assigned tool directories, plus NEW files (new icons, new private components).
  Do not edit the shared kit files listed above, TrailSensePage.kt, the theme, MainActivity or the nav host — if
  you need a kit change, describe it in your final report.
- Strings: add new en-US strings only in a NEW file app/src/main/res/values/strings_<your-group>.xml. Never edit
  other locales, `guides/`, or `scripts/`.
- No emulator: compile only. Don't push.
