# Round 8: fix the reviewer's concrete defects

Read first: .sisyphus/plans/trail-sense-style.md (the style; it wins every conflict), then round7.md (process and
rules, all still apply). The Sonnet design review of the current build is in /tmp/ts/sonnet7_A.md … sonnet7_D.md
(per-screen: worst / fix1-3 / delight, then a Cross-screen section). Current captures: /tmp/ts/r7/*.png — look at
your screens' PNGs before changing them.

Your job: for every screen you own, fix the "worst" defect and whichever of fix1-3 fit the style. Ignore advice that
adds decoration, a second hero, custom hues, or extra animation; the style says calm. Prefer removing to adding.
Ignore hue complaints (colours come from the Material scheme). Ignore "delight" unless it is tiny and carries meaning.

## Cross-screen rules (everyone)
- **One value, one action, one object.** Never show the same number/text/icon twice on a screen.
- **Selection:** one look everywhere: the kit's connected button group / segmented control (secondaryContainer
  fill + check for the selected item, fixed label position, 40 dp). Owner of the kit change: agent A. Others: use
  the kit component, don't restyle selection locally.
- **Empty states** use `TrailSenseEmptyState`; agent A makes it sit at a fixed optical position (~40% height).
  Others just make sure their empty screens use it with one in-flow action and no FAB while empty.
- **Top app bar:** large title for Calm lists and Instruments; small bar only for full-bleed camera/light screens
  (Flashlight, Magnifier). Lightning gets the large title like the other instruments.
- **State colours:** errorContainer/error for real danger only; tertiaryContainer/onTertiaryContainer for warnings
  and attention; normal rows onSurfaceVariant. No green/orange literals for status.
- **Single-item groups:** merge into a neighbour group or drop the header.
- **Leading icons must differ in meaning;** if all rows would get the same icon, drop the icons.
- **Idle instruments** show the live object at rest with one hint line, never a big dash or "Waiting" header.
- **Inputs** look like inputs (real filled/outlined fields with labels).

## Ownership (only touch files for your screens; shared kit = agent A only)
- **A:** Astronomy, Ballistics, Battery, Beacons, Bubble Level, Climate, Clinometer, Clock, Clouds, Convert
  (sonnet7_A.md) + the shared kit (shared/.../views/compose/*): selection component, empty-state position,
  list-row spec. Do the kit first and commit it early.
- **B:** Declination, Diagnostics, Experimentation, Field Guide, Flashlight, Light Meter, Lightning, Magnifier, Map,
  Metal Detector (sonnet7_B.md).
- **C:** Navigation, Notes, Offline Maps, Packing Lists, Paths, Ruler, Sensors, Settings, Signal Finder, Solar
  (sonnet7_C.md). Plus beacon follow-ups: show the beacon's mark (`BeaconShapeMark`, tools/beacons/ui/BeaconShapeSheet.kt)
  on the beacon details page header; the beacon editor title says "Edit beacon" when editing (add the string to
  values/ en-US only).
- **D:** Survival Guide, Temperature, Tides, Triangulate, Turn Back, User Guide, Water Boil Timer, Weather, Whistle,
  White Noise (sonnet7_D.md). Tides: fix the "Low" vs "Falling · Next: Low" contradiction and the now dot collision.
  Weather: keep the pressure gauge icon beside the pressure value (the user asked for it).

## Don'ts
- Don't touch scripts/, guides/, or non-en-US locales. Don't change hues or TrailSenseTheme.
- Don't merge origin/main. Don't push.
- Behaviour, test tags and accessibility semantics stay.

## Done means
Compile output truly empty (`ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ./gradlew :app:compileDebugKotlin -q`),
`:app:mergeDebugResources -q` passes, commits on your worktree branch. Report: your branch name, then one line per
screen: what defect you fixed and what you removed.
