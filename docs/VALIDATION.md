# Validation evidence

Android/UI checks updated in the managed Linux cloud environment on 22 October
2026. The native decoder sample checks below were performed on 21 October 2026;
the audio decoder is unchanged.

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

Nineteen JUnit/Robolectric tests, zero failures and zero skipped:

- Empty library, disabled seek control, and Android document-picker launch.
- Bidirectional title scrolling: edge pauses, smooth reversal, unchanged-title
  updates, and short titles remaining still.
- No artwork UI or fixed 16-bit label; borderless mode buttons and inset seek bar.
- Persistent library restoration, track names/durations, and paused-player progress.
- Format recognition and time labels.
- Two-column theme preview grid, selected-card indication, selection and persistence
  across activity recreation.
- Removed subtitle/section label and full-width Library button layout.
- Readable text/button contrast across all 12 palettes, plus stable preference IDs.
- Tagged-track straight/loop controls and per-track preference overrides.
- Shuffle next-track behavior and playback-mode persistence across service restart.
- xid6 loop-tag parsing, padded fields, intro-only tags, and malformed metadata.
- FFT tone-band placement, silence, amplitude response, and stereo phase handling.
- Media service creation, binding, and teardown on Android API 35.
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
- Corrupt SPC and RSN rejected.
- Oversized extraction rejected and partial files cleaned up.
- Archive paths could not escape the destination directory.

The music samples remain outside the repository and APK. Game artwork and
matching code are removed; the APK is checked for absence of the old artwork pack.

## Screenshots and remaining device checks

`docs/screenshots/` contains native-graphics Robolectric renders of the real Java
view hierarchy. The soundtrack images use a seven-track metadata fixture; the
paused player image uses a service-state fixture at 1:13. Theme images additionally
use synthetic multitone PCM passed through the actual FFT and visualizer, rather
than captured device audio on every system. The track-row loop tag shown for the fixture is
synthetic too; sample music is not modified. All 12 theme renders retain the
selected track and progress and omit the removed footer and adventure text. These are not emulator
or physical-device captures, and do not establish Android speaker output.

No emulator or physical Android device was used. A device listening pass remains
for audio routing, Bluetooth, interruptions, notification/lock-screen controls,
and background behavior. The additional GME console formats are compiled in but
do not have sample-based functional validation in this run.

The included APK is debug-signed for sideloading; Play Store release signing and
publication have not been performed. A GitHub Actions workflow is supplied for
future builds; local checks do not prove a remote Actions run succeeded.
