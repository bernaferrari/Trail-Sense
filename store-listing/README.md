# Trail Sense · tool screens

Made with [Screenshot Studio](https://github.com/bernaferrari/screenshot-studio). For now it captures every tool
screen in light and dark, so design rounds can be compared side by side; there are no store cards yet
(`gallery.json` is empty).

## Capture every tool

On the capture emulator (`emulator-5578`, the `Resizable_Experimental` AVD), from the repository root:

```sh
npx screenshot-studio recapture                    # build the debug app, install it, capture every tool in both themes
npx screenshot-studio tour --tools ruler,clock     # some tools only, on the installed app (both themes)
```

Each tool lands in `en-US/raw/phone/<tool>.png` (light) and `<tool>-dark.png` (dark), with its `.nodes.json` layout
map. Captures are not committed; copy the folder aside to keep a round.

`capture.tour.mjs` does it the way a person would: each theme starts with the app's data cleared, steps through
onboarding, picks Light or Dark in Settings → Appearance, then opens each tool with the app's own
`com.kylecorry.trail_sense.OPEN_TOOL` intent and taps away first-run dialogs ("OK", "Got it", "I agree", "Continue").
A tool the device doesn't offer is skipped with a warning. The studio sets the emulator up (screen size, 9:41 status
bar, no network) and puts it back afterwards; it never runs on a phone.

| File | Holds |
| --- | --- |
| `capture.tour.mjs` | The tools (id and name) and how each is opened |
| `studio.json` | App name, language (en-US only for now), the build and APK `recapture` installs |
| `profiles.json` | The phone capture size: 1080 × 2340 at 400 dpi |
