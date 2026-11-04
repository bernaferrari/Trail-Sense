# Round 10: the boiler principle, and the user's latest notes

Read first: .sisyphus/plans/trail-sense-style.md, especially "The boiler principle" at the end, and round9.md
(the family language, kit components and process; all still apply).

The user loves the Water Boil Timer (tools/waterpurification/ui/WaterPurificationScreen.kt). Its object is a pot of
water that fills, ripples and counts down. It has one instruction line, one choice row and one TrailSenseButton.
"We should have more things like the boiler." Study that file first, and reuse its drawing and motion approach.

## Kit you must use (no one-offs)
TrailSenseButton (Filled/Tonal/Outlined/Text), TrailSenseRowWell, TrailSenseListItem, TrailSenseInputField,
TrailSenseSheet, TrailSenseDialog, TrailSenseEmptyState, TrailSenseConnectedChoice, TrailSensePage / instrument
scaffold, trailSenseSection. Google Sans Flex is the theme font. The bottom bar is always visible and owns the inset.

## The user's notes
- **Lightning Strike Distance:** "make it as good as the boiler". Today it's a static grey circle with "0.0 s".
  Make the object live:
  - The flash lights it.
  - Sound rings spread outward at the speed of sound until the thunder tap.
  - The result is a distance, with a calm or danger verdict (a strike within ~10 km means danger: seek shelter).
  - One instruction line, one TrailSenseButton that changes label (Lightning → Thunder → Again), no chrome.
- **Pedometer:** "can be redesigned to look better". A boiler-like object: e.g. a path or trail that fills with
  footsteps toward the distance alert goal (or a ring of steps). The step count inside it, the distance under it, and
  Start/Reset as TrailSenseButtons. Speed and pace stay as quiet rows below.
- **Metal Detector:** "a bit weird, especially at the bottom". Today the bottom is a "Magnetic field" card with a
  lone "Metal" label and a mostly empty chart. Make the bottom calm and meaningful: remove the empty chart card, or
  make the trace an integral part of the field well. Accuracy is one quiet line. The well itself is good.
- **Ruler:** "can be redesigned to look better". The ruler strip is good. Redesign the side panel: one clear
  readout of the measured length (display type), the cm/in choice, and map scale as a tidy group that doesn't look
  like a form.
- **Triangulate Location:** "can be redesigned to look better". Still form-like. Think boiler: the object could be
  a small map/compass showing the sighting lines crossing as you take them (the intersection appears once there
  are two). Keep the step-by-step flow and test tags.
- **Tides:** "nearby" is confusing while loading. Show a neutral loading state (no table name until known) and say
  plainly what "Nearby" means once loaded (e.g. "Nearest tide table: <name>"). Keep refining the rest.

- **Climate:** "still looks MUCH worse than (Not Boring) apps. Way too much text, everything is confusing; I have
  no idea what's happening. Two temperature graphs is much worse than today's weather apps, which show a min/max
  gradient for each day."
  - Redesign from scratch, like iOS Weather's 10-day list or (Not Boring) Weather.
  - One row per month: the month name, the low, a rounded gradient capsule from low to high on one shared scale
    across all months (cool to warm, with colour derived from the scheme or temperature), then the high.
  - The selected month (the current one by default) is emphasised; tap a month to select it.
  - Above the list, one hero answer for the selected month: e.g. "18° / 7°" in display type, with one verdict line
    ("Mild, wet: 92 mm of rain").
  - Precipitation is a quiet value or a small droplet bar per row, not a second chart.
  - Remove every other chart, legend and paragraph. Keep location/elevation as one small tappable line.

## Process
- Worktree at local `main` (`git reset --hard main` if not; never origin/main). Copy local.properties from
  /Users/bernardoferrari/Documents/kotlin/Trail-Sense.
- Compile with ANDROID_HOME=/Users/bernardoferrari/Library/Android/sdk ./gradlew :app:compileDebugKotlin -q (the
  output must be empty), then run :app:mergeDebugResources -q.
- en-US strings only. Keep test tags. Commit per screen.
- Don't use the phone or the emulator; the lead verifies on the phone. Current captures: /tmp/ts/p10/<Name>.png.
- Report: your branch, and one line per screen.
