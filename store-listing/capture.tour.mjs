// Every Trail Sense tool screen in light and dark, on the capture emulator, to compare design rounds. It drives the
// debug build (./gradlew :app:assembleDebug, com.kylecorry.trail_sense) with no fixtures: each theme starts with the
// app's data cleared, walks the first-run screens, picks Light or Dark in Settings → Appearance, then opens each tool
// with the app's own OPEN_TOOL intent. Scenes are independent, so any subset runs on its own.
// Everything: npx screenshot-studio recapture (build, install, capture)
// Alone:      npx screenshot-studio tour [--tools ruler,clock]   (tool names as below; both themes)

// Tool ids from tools/tools/infrastructure/Tools.kt. Local Messaging (39) and Local Talk (40) are left out: they
// need the separate Trail Sense Comms app, so the app never shows them. Experimentation (36) is a debug-only
// scratch screen, not part of the app.
const TOOLS = {
  flashlight: 1, whistle: 2, ruler: 3, pedometer: 4, 'cliff-height': 5, navigation: 6, beacons: 7,
  'offline-maps': 8, paths: 9, 'triangulate-location': 10, clinometer: 11, 'bubble-level': 12, clock: 13,
  astronomy: 14, 'water-boil-timer': 15, tides: 16, battery: 17, 'solar-panel-aligner': 18, 'light-meter': 19,
  weather: 20, climate: 21, 'temperature-estimation': 22, clouds: 23, 'lightning-strike-distance': 24,
  'augmented-reality': 25, convert: 26, 'packing-lists': 27, 'metal-detector': 28, 'white-noise': 29, notes: 30,
  'qr-code-scanner': 31, sensors: 32, diagnostics: 33, settings: 34, 'user-guide': 35,
  'mirror-camera': 37, 'turn-back': 38, 'survival-guide': 41, 'field-guide': 42, 'signal-finder': 43,
  ballistics: 44, declination: 46, map: 47, magnifier: 48,
};

// First-run dialogs and disclaimers, by their button (English: other languages need their words here).
const DISMISS = /^(?:OK|Got it|I agree|Continue)$/i;
// Granted up front, so no system permission dialog covers a tool. pm clear revokes them again each theme.
const PERMISSIONS = ['ACCESS_FINE_LOCATION', 'ACCESS_COARSE_LOCATION', 'ACCESS_BACKGROUND_LOCATION', 'CAMERA',
  'ACTIVITY_RECOGNITION', 'POST_NOTIFICATIONS'];

const grant = d => { for (const p of PERMISSIONS) try { d.shell('pm', 'grant', d.app, `android.permission.${p}`); } catch {} };

/** Tap away whatever first-run dialogs are up (a few in a row at most). */
async function dismiss(d) {
  for (let i = 0; i < 3; i++) {
    const button = (await d.tree()).find(n => DISMISS.test(n.text || ''));
    if (!button) return;
    const [l, t, r, b] = button.b;
    await d.tapAt((l + r) / 2, (t + b) / 2);
  }
}

/** Open a tool the way the app's shortcuts do; false when this build or device doesn't offer it. */
async function open(d, id) {
  const before = (await d.tree()).map(n => `${n.text}|${n.desc}`).join();
  d.shell('am', 'start', '-W', '-a', 'com.kylecorry.trail_sense.OPEN_TOOL', '-n', `${d.app}/.main.MainActivity`, '--el', 'tool_id', id);
  await d.settle(6000);
  await dismiss(d);
  if ((await d.tree()).map(n => `${n.text}|${n.desc}`).join() === before) {
    console.warn(`Tool ${id} did not open (not available here?), skipped`);
    return false;
  }
}

const wanted = (d, name) => {
  const only = d.options.tools;
  return !only || String(only).split(',').map(s => s.trim()).includes(name);
};

/** The Tools tab itself, the app's hub: a cold start, then its bottom-bar item. */
async function toolsTab(d) {
  await d.launch();
  await dismiss(d);
  await d.tap('Tools');
  await d.settle(3000);
}

const scenes = suffix => ({
  ['tools-tab' + suffix]: d => wanted(d, 'tools-tab') ? toolsTab(d) : false,
  ...Object.fromEntries(Object.entries(TOOLS).map(([name, id]) =>
    [name + suffix, d => wanted(d, name) ? open(d, id) : false])),
});

/** @type {import('screenshot-studio').Tour} */
export default {
  app: 'com.kylecorry.trail_sense',
  activity: '.main.MainActivity',
  // A fresh start: onboarding (units, the disclaimer), then the theme from Settings → Appearance.
  async start(d) {
    await d.launch({clear: true});
    grant(d);
    await d.tap('Next');
    await d.tap(/I agree/i);
    if (await d.isVisible('Next')) await d.tap('Next');  // the missing-compass page, on devices without one
    await d.waitUntil(() => d.topActivity().includes('.main.MainActivity'), {message: 'Trail Sense never left onboarding'});
    await d.settle(6000);
    await dismiss(d);
    await open(d, TOOLS.settings);
    await d.tap(d.theme === 'dark' ? 'Dark' : 'Light');
    // The choice is saved at once; a cold start shows it everywhere, with no back stack left from Settings.
    await d.launch();
    // Recreating MainActivity for the new theme can crash ("Navigator is not attached"); the app then offers a
    // report on the next start. Say so, and clear it so it doesn't cover every tool.
    if (await d.isVisible('Send report')) {
      console.warn('Trail Sense crashed while changing theme (see adb logcat -b crash); dismissed its crash report');
      await d.tap('Not now');
    }
    await dismiss(d);
  },
  scenes: {
    light: scenes(''),
    dark: scenes('-dark'),
  },
};
