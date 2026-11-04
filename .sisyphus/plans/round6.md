# Round 6: from "polished" to "people love it"

Read .sisyphus/plans/expressive-pass.md first (kit, rules of taste, boundaries — all still apply).
A stricter Sonnet jury reviewed round 5: average ~6.7/10. Its full notes are in /tmp/ts/review6_sonnet.md
(per screen: score, idea, worst, fix1-3, delight, plus cross-screen sections). Current captures: /tmp/ts/r5/<Tool>.png.
References (the bar): /tmp/ts/ref/*.png — look again before you start.

## What changed in the kit (already on main)
- `TrailSenseEmptyState(..., tone, accents: List<ImageVector>, secondaryActionLabel, onSecondaryAction)`: the
  mark takes the feature's tone, and up to 3 accent icons ride the satellites so each empty screen tells its own
  little story (Notes: pencil + lines; Packing: backpack + check; Map: pin + layers...). Every empty state you
  own must pass a fitting tone and 2-3 accents. No two empty screens should look alike.
- `TrailSenseSearchBar(containerColor = ...)` is tinted by default with a primary glyph.
- `TrailSenseTileGrid`: a short last row now stretches across the full width.

## The jury's themes (act on all of them for your screens)
1. **Every screen expresses ONE idea** (the review's `idea:` line). The hero/dial/chart IS that idea; cut or
   demote anything that doesn't serve it. Don't repeat the screen title as the hero eyebrow; don't show the same
   number twice.
2. **Colour carries meaning, not decoration.** Not every hero is mint: pick the hero tone from the state (Good when
   fine/aligned/level, Caution when near a threshold, Error for alerts, Tertiary for warm/sun/high, Secondary for
   neutral/idle). Animate tone changes with the motion scheme.
3. **Designed idle states.** No bare "--", "— °C", "--:--", flat empty charts or 0-dash. Idle shows an instruction
   headline, a ghost/dashed placeholder, or a calm illustration; hide markers/thumbs until a value exists.
4. **Instruments look alive and high-contrast.** Dials, rulers, radars, gauges: onSurface labels, primary needles,
   filled bands, real proportions. No washed-out greys. Each gets a graphic partner for its big number.
5. **One consistent shape grammar.** Connected groups: 24dp outer / 6dp inner, 2dp gaps — fix any square join or
   mismatched corners (button trios, sound grids, stat tiles). Scallops/cookies for identity/status, circles
   for actions.
6. **Delight.** For each screen implement the review's `delight:` idea (or a better one) when it is buildable and
   tasteful: springy needles, a badge that slowly turns, numbers that roll, a sweep, a pulse on success. Keep it
   subtle and use MaterialTheme.motionScheme. Respect reduced motion where an API exists.

Ignore these jury notes (they misread the capture): content scrolling under the gesture bar at the bottom of a
long list is normal edge-to-edge; sensor values of 0 on the emulator; Tides was captured before data loaded (its
loaded state already has a curve — still design its loading state).

Push past the listed fixes. Ask yourself for each screen: "would someone screenshot this and post it?" If not,
what single move would make them? Do that.

Compile with `./gradlew :app:compileDebugKotlin -q`. Commit in small groups in your worktree. Final report:
per screen, one line on what changed, plus any kit wishes.
