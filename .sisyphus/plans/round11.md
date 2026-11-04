# Round 11: the user's notes on the round-10 build

Read round10.md first (the boiler principle, the kit, the process: all still apply). Captures of the current build on
the user's phone: /tmp/ts/p11/<Name>.png.

## The user's notes
- **Bubble Level:** "ugly because the bubble looks like a thumb thing from iOS". The bubble must read as a real air
  bubble in liquid. Avoid anything that looks like a slider thumb or toggle knob. Use a translucent bubble with a soft
  highlight and refraction edge in a liquid-filled vial, the vial in a tonal container, and the level marks as engraved
  lines. Keep the flat and edge modes.
- **Lightning Strike Distance:** "should allow me to tap in the middle circle (same as start/stop)". The sky object
  is itself the button: tapping it does what the main button does, with a ripple or press scale and proper
  semantics. Keep the button too.
- **Light Meter:** "can be refined, but it is getting good". Polish the sun object and the hierarchy; don't restart.
- **Signal Finder:** "can be redesigned / refined / made better for M3 Expressive". Make the radar the object, with
  expressive shapes for towers, a clear strongest-signal answer, and calm rows.
- **Climate:** "the monthly thing is weird, not as good as Today Weather yet". Today Weather (a weather app on the
  user's phone) shows 7 day columns: the high above, a thin vertical gradient bar (3-4 dp, rounded, warm gradient)
  floating at its range on a shared scale, the low under it, then rain % / icon / day name.
  - Make Climate's year card match that clarity: thinner bars (~6 dp), floating at their range, the high and low
    labels hugging the bar ends, generous column spacing, month labels at the bottom, a quiet rain row.
  - Remove anything else in the card. The hero stays.
- **Temperature Estimation:** "super ugly / weird, you can do better". Redesign from scratch. One question: "how
  warm will it be where I'm going?". Make the object a clear before/after (here → there) with the two temperatures
  as the hero, the elevation change as the link between them, and inputs as calm rows or a sheet. No odd hill drawing
  unless it is genuinely beautiful and clear.
- **Astronomy:** "can be further polished / prettified".
- **Sensors (GPS row):** "Moderate · 2 satellites is super ugly" (SensorDetailsScreen.kt ~376 builds
  "status\nN satellites"). Make it one clean line, e.g. a status word plus a quiet "2 satellites" as the supporting
  text, or fold the satellites into the value. No two-line status jammed in a cell.
- **Declination:** "Magnetic to true is a bit confusing". Rewrite the choice in plain words, e.g. "My bearing is
  from: Compass / Map", with one sentence saying what to add or subtract.

## Process
As in round10.md: a worktree at local main, local.properties, compile empty, mergeDebugResources, en-US only, keep or
update test tags, commit per screen. No phone or emulator. Don't edit the shared kit; ask the lead.
