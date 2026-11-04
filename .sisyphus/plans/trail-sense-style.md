# The Trail Sense style (proposal)

## Why it still feels overwhelming
We raised every screen to a "hero": a saturated card, a shape badge, an eyebrow, a big number, a verdict, chips,
satellites, a pulse. Material 3 Expressive says the opposite: **one or two hero moments per product**, Foundational
intensity everywhere else, and expressiveness used to make the key thing findable, not to decorate. When 41 screens
all shout, nothing is emphasised.

## Principles
1. **One question per screen.** Each screen answers one question ("Is there metal here?", "Which way is camp?",
   "Will the weather turn?"). The answer is the only emphasised thing. Anything else sits below it, in a quiet list,
   or in a sheet.
2. **Three intensities, and nothing in between.**
   - **Calm** (most screens: lists, guides, settings, forms, empty states). A standard top app bar, a plain surface,
     and grouped lists (24/6 dp, 2 dp gaps). Emphasised type for the single key value. No hero card, no badge,
     no satellites, and no chips that don't do anything.
   - **Instrument** (screens whose answer is a live reading: Navigation, Bubble Level, Clinometer, Metal Detector,
     Light Meter, Clock, Whistle, Water Boil Timer, Solar, Declination, Signal Finder). One live object (a dial,
     well, ring or ruler) on a plain surface. The object carries the colour, not the page. The reading sits inside it
     or right under it, with the verdict as headlineMedium.
   - **Hero moments** (only two in the whole product):
     - *You're facing it.* On Navigation, the turn arc closes, the destination badge morphs from Cookie9 to Cookie12,
       and the phone gives one haptic tick.
     - *The weather turns.* The Weather hero turns errorContainer for a storm alert.
3. **Colour comes from the Material scheme's roles.**
   - primary is for the live object and the main action.
   - surface and surfaceContainer* are for everything else.
   - error is for real danger only.
   - tertiary is for at most one accent per screen ("now" on a chart).
   - No container slabs behind whole sections, and no nested cards.
4. **Shape.** Expressive shapes appear only on the live object and as list leading icons, with one shape family per
   screen. Shape changes mean a change of state (the level ring rounding when level, the metal core sharpening near
   metal).
5. **Type.** One display value per screen. Everything else uses title and body styles. Never repeat the screen title
   as an eyebrow, never show the same number twice, and no decorative icons beside names (the arrow already says
   "destination").
6. **Charts.** Use `TrailSenseChartFrame`: word labels in a gutter, one line in primary, dashed limits, a "now" dot.
   No bands and washes stacked on top of each other.
7. **Empty states.** An icon in a cookie, one sentence and one filled button. Drop the satellites.
8. **Motion.** Use springs for state changes (needles, morphs, numbers). An idle loop is allowed only where it
   carries information (the metal well's ripple speed is the field strength). Honour the system animation scale.

## Intensity map
| Intensity | Screens |
|---|---|
| Hero moment | Navigation (facing the destination), Weather (storm alert) |
| Instrument | Navigation, Bubble Level, Clinometer, Metal Detector, Light Meter, Clock, Whistle, Water Boil Timer, Solar Panel Aligner, Declination, Signal Finder, Ruler, Lightning |
| Calm | Tools, Settings, Beacons, Paths, Notes, Packing Lists, Map, Offline Maps, Field/Survival/User Guide, Sensors, Diagnostics, Experimentation, Battery, Convert, Ballistics, Climate, Clouds, Tides, Astronomy, Temperature, Turn Back, Triangulate, Magnifier, Flashlight, White Noise |

## What changes on a Calm screen, concretely
- The hero card goes. Its one number becomes emphasised type at the top of the page (displaySmall SemiBold, in
  onSurface), with one line of verdict under it.
- The badges, eyebrows and pills in the header go.
- The main action becomes one filled button (or FAB) in primary, and nothing else on the screen uses primary as a fill.
- The rest of the screen is grouped lists.

## Done already (prototypes of this style)
- **Metal Detector** (Instrument): the field well with the reading inside, the verdict, and a quiet trace.
- **Navigation:** beacons are ringed waypoint dots, there's no pin before the name, the arc's caps sit on the wedge,
  and the moon is a quiet badge.
- **Weather and Metal Detector charts** use the frame.
