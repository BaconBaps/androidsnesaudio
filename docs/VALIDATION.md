# Validation evidence

Android/UI checks updated in the managed Linux cloud environment on 24 October
2026 (Europe/London). Native decoder sample checks below were performed on
22 October; this visual update does not change the decoder.

## Command-line Android build

- JDK: Eclipse Temurin 21.0.8+9.
- Gradle wrapper: 8.10.2, SHA-256 pinned.
- Android Gradle Plugin: 8.7.3; compile/target SDK 35; minimum SDK 26.
- NDK: 27.2.12479018; CMake: 3.22.1.
- APK architectures: arm64-v8a and x86_64.
- `scripts/setup-cloud.sh`: executed successfully, then repeated successfully.
- `scripts/build.sh`: assembles a debug APK, runs unit tests, and runs Android lint.
- APK signature verified with `apksigner`; 16 KB ZIP alignment verified with `zipalign`.

## Automated tests

Fifty-five JUnit/Robolectric tests, zero failures and zero skipped:

- Empty library, disabled seek control, and Android document-picker launch.
- Bidirectional title scrolling: edge pauses, smooth reversal, unchanged-title
  updates, and short titles remaining still.
- No artwork UI or fixed 16-bit label; borderless mode buttons and inset seek bar.
- Persistent library restoration, track names/durations, and paused-player progress.
- Format recognition and time labels.
- Two-column theme preview grid, selected-card indication, selection and persistence
  across activity recreation.
- Removed subtitle/section label and full-width Library button layout.
- Readable text/button contrast across all 10 palettes, plus stable preference IDs.
- Tagged-track straight/loop controls and per-track preference overrides.
- Shuffle next-track behavior and playback-mode persistence across service restart.
- xid6 loop-tag parsing, padded fields, intro-only tags, and malformed metadata.
- FFT tone-band placement, silence, amplitude response, and stereo phase handling.
- Media service creation, binding, and teardown on Android API 35.
- Incremental seeking: 250 ms maximum forward steps, cancellation/replacement,
  backward restart with mode restoration, sample-rounded arbitrary targets,
  and rejection of genuinely stalled decoder progress.
- Service-level seek recovery with a sample-rounded decoder and a full audio sink:
  output resumes automatically with Loop off/on, a pause interrupts an active seek,
  paused seeking stays paused, and a subsequent play resumes output. This uses
  Robolectric shadows, not physical speaker output.
- The time display shows and clears the SEEKING target while catching up.
- Submenu Import picker, centered empty-only tracklist Import, Library Delete
  confirmation, last-entry empty state, and Exit menu/task closure.
- ChipPlayer 199x launcher label and wordmark; flat outer player background.
- Flat heading with a raised menu button; retired themes fall back to Pixel Light.
- One centered menu button in the header; playlist above the playback
  deck; spectrum inside the deck and before the seek/readout controls.
- Enlarged Library viewport, platform expansion/collapse persistence, and
  correct soundtrack selection after grouping and sorting.
- Platform aliases, preserved duplicate imports, alphabetic grouping, unknown
  metadata, and mixed-system archives.
- Darker playback colours retain readable text and accents across all 10 themes.
- Duplicate deletion persists across library reload, removes only the chosen
  private set and its loop preferences, retains the other import/source, and
  rejects folder traversal.
- Explicit Stop releases decoder/audio output, clears queue and notifications,
  completes its callback, and permits later playback.
- Six image borders compile as real Android nine-patches; fixed corner pixels
  remain unchanged across sizes, and control presses change edging without
  changing the center or layout.
- Pixel-sampled textured chassis and selected rows keep at least 4.5:1 text
  contrast across all 10 themes. Llama is featured in the picker, and theme
  selection still survives recreation.
- Fixed game/metadata/Tracklist headers during playlist scrolling; soft light-theme
  button frames verified by rendered pixel contrast.
- Notification Exit action releases audio and removes the notification; colorization
  is disabled and no large artwork is attached. Android controls final media notification presentation.
- WAV header, exact sample count/duration, little-endian stereo, fades, bounded
  offset seeks, cancellation and decoder closure on output failure.
- Export time validation, full-track shortcut, document-picker MIME/filename,
  and clip settings across Activity recreation. No sound-setting permission.
- Favourite persistence, subtrack/duplicate identities, deletion cleanup,
  cross-game filtered UI, empty state and Activity recreation.
- Sleep expiry with Loop on pauses and keeps position; cancellation permits
  continued playback and Stop clears a pending timer.
- Dimmed list palette, readable secondary text/selected tracks and lighter
  Library-button contrast across all ten themes.
- Fixed tracklist frame outside the ScrollView; rendered bottom-border pixels
  remain identical at the top, middle and bottom of a long playlist.
- Vertically centered section text and disabled baseline alignment; no Local
  files label. Existing fixed-header scrolling checks still pass.
- Native RemoteViews render empty and playing widgets, preserve palette/clock/
  loop labels, route buttons to the foreground playback service, and open the app
  when empty. A launcher update and stale service tap clear controls safely.
- Widget Previous/Play-Pause/Next commands control the same decoder and queue.
- ZIP traversal protection and preserved display names.
- Track-count limit with partial-import cleanup.
- Clear rejection of archives containing no supported music.

Native sample command:

```sh
scripts/test-samples.sh '/path/to/Donkey Kong Country (EMU).zophar.zip' /path/to/sf2t.rsn
```

Results:

- Donkey Kong Country ZIP: 29 SPC files extracted and decoded.
- Street Fighter II Turbo RSN (solid RAR4): 36 SPC files extracted and decoded.
- All 65 tracks passed Java/JNI metadata, non-silent 44.1 kHz stereo PCM, seeking
  to 15 seconds, position reporting, and subsequent rendering checks.
- One representative SPC from each sample set also passed a continuity comparison:
  enabling Loop during playback produced identical PCM to an uninterrupted,
  unfaded reference for over 12 seconds, past an artificial 1-second ending time
  and the ordinary fade boundary. Disabling Loop restored the fade and ending;
  backwards seeking followed by mode reapplication preserved looping.
- Generated original-tone Mega Drive VGZ (PSG + YM2612-enabled container) and
  upstream GME `test.vgz` both passed forward/backward incremental seeks at
  60, 15, 0 and 30 seconds plus arbitrary 12347, 5033 and 9981 millisecond
  targets, with Loop off and on. Each jump resumed non-silent
  PCM. Cancelling a pending seek allowed a new decoder to render and advance.
  Neither sample is bundled.
- Corrupt SPC and RSN rejected.
- Oversized extraction rejected and partial files cleaned up.
- Archive paths could not escape the destination directory.

The music samples remain outside the repository and APK. Game artwork and
matching code are removed; the APK is checked for absence of the old artwork pack.

## Screenshots and remaining device checks

`docs/screenshots/` contains native-graphics Robolectric renders of the real Java
view hierarchy. The grouped Library image uses four imports across three platforms; the library
deletion image uses a one-track fixture; the soundtrack images use a seven-track metadata fixture; the
paused player image uses a service-state fixture at 1:13. Theme images additionally
use synthetic multitone PCM passed through the actual FFT and visualizer, rather
than captured device audio on every system. The track-row loop tag shown for the fixture is
synthetic too; sample music is not modified. All 10 theme renders retain the
selected track and progress and omit the removed footer and adventure text. These are not emulator
or physical-device captures, and do not establish Android speaker output.

No emulator or physical Android device was used. A device listening pass remains
for audio routing, Bluetooth, interruptions, notification/lock-screen controls,
and background behavior. The exact VGZ reported by the user was not supplied,
so its device-specific freeze has not been reproduced. A representative Mega
Drive VGZ reproduced expensive synchronous seeking on the shared control worker;
seeking now yields between 250 ms decoder steps. A follow-up native regression
  reproduced failure at arbitrary scrub targets: GME truncates sample positions
  to milliseconds, and the completion check incorrectly rejected a 1 ms gap.
  The check now tolerates that conversion gap while retaining stall detection. AudioTrack writes are also
non-blocking and preserve partial buffers. Other console formats beyond SPC and
VGZ do not have sample-based functional validation in this run.

The included APK is debug-signed for sideloading; Play Store release signing and
publication have not been performed. A GitHub Actions workflow is supplied for
future builds; local checks do not prove a remote Actions run succeeded.

## Offline export checks (23 October 2026)

`tests/NativeExportSmoke.java` exercised the real JNI decoder with the supplied
Donkey Kong Country SPC set and a VGZ sample, each at offsets 0 and 12,347 ms.
All four exports produced 358,664-byte WAVs for 2,033 ms clips, with audible
samples and a final fade to zero. A simultaneously open playback decoder kept
its original position. These host checks validate file rendering, not Android
speaker output. `scripts/test-samples.sh` includes this export check.

The export and favourites screenshots are native-graphics Robolectric renders.
Export jobs retain their state across configuration changes, are cancellable,
and use a temporary file before copying to the chosen document provider. Final
provider behavior and lock-screen sleep timing still need physical-device checks.

## Home-screen widget

`docs/screenshots/homescreen-widget.png` is a native-graphics Robolectric render
of the actual RemoteViews hierarchy at 360 × 220 dp. API 26 uses the base provider
metadata; API 31+ adds a layout preview and four-by-three target cells. No periodic
widget wakeup is configured: the launcher drives the elapsed clock, and playback
or theme changes refresh the remaining content. Launcher placement, resizing,
and background restrictions still need verification on a physical device.
