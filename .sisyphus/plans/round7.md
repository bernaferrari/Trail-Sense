# Round 7: apply the Trail Sense style (calm by default)

The user approved .sisyphus/plans/trail-sense-style.md. Read it first. It overrides earlier rounds wherever they
conflict (rounds 5 and 6 escalated every screen into a hero; that made the app overwhelming). Also read
.sisyphus/plans/expressive-pass.md for the kit and the boundaries, which still apply.

Your job: take every screen you own to its intensity in the style's map, by **removing** more than by adding.
Each screen should feel considered, as if a Google Material designer had cut it down to its one question.

## For every screen
1. Write the screen's one question as a comment at the top of its main composable.
2. Decide what answers it. That is the only emphasised element.
3. **Calm screens:**
   - Remove the TrailSenseSpotlight / hero card and any saturated container slab. The key value becomes
     displaySmall SemiBold onSurface at the top of the content, with one bodyLarge/titleMedium verdict line.
   - Remove badges, eyebrows, decorative chips, pulses and idle animations.
   - The rest of the screen becomes grouped lists (trailSenseSection / TrailSenseListItem, 24/6, 2 dp gaps).
   - The main action is one filled primary button or FAB. Nothing else uses a primary fill.
4. **Instrument screens:**
   - One live object (dial, well, ring) on the plain page; it carries the colour.
   - The reading sits inside it or directly under it, with the verdict as headlineMedium.
   - Controls go below in one grouped list.
   - No card around the instrument and no second hero.
   - See Metal Detector (tools/metaldetector/ui) as the reference.
5. **Charts** use `TrailSenseChartFrame` (shared kit, TrailSenseChart.kt): words in a gutter, one primary line,
   `trailSenseSmoothPath`, `drawTrailSenseNowDot`. No stacked bands and washes, and no numeric dashed grids.
   See Weather's pressure chart.
6. **Empty states:** `TrailSenseEmptyState(icon, title, description, actionLabel, onAction)`, with one action.
   Stop passing `accents` (now ignored) and remove those arguments. No FAB while the list is empty, unless the
   FAB IS the action.
7. **Colour:** scheme roles only. primary is the live object and the main action, error is danger only, tertiary is
   one accent at most. No custom Color(...) literals in UI except data colours (beacon/path colours the user chose).
8. **Type:** no eyebrow that repeats the title, no number shown twice, no decorative icon beside a name.
9. Keep behaviour, test tags and accessibility semantics. Don't remove features: move secondary ones into the list or
   an overflow.

## Process
- First `git merge main` in your worktree. **Never merge origin/main**: upstream commits must stay out. If the
  worktree started from origin/main, reset it to main first.
- Compile with `ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ./gradlew :app:compileDebugKotlin -q`.
  The output must be truly empty. Copy local.properties from /Users/bernardoferrari/Documents/kotlin/Trail-Sense if
  it's missing (it's gitignored).
- Also run `ANDROID_HOME=... ./gradlew :app:mergeDebugResources -q` once at the end (duplicate strings fail only there).
- Commit per screen or small group. Final report: one line per screen (its question, and what you removed).
