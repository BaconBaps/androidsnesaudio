# Validation evidence

Executed in the managed Linux cloud environment on 21 October 2026.

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

Ten JUnit/Robolectric tests, zero failures and zero skipped:

- Empty library, disabled seek control, and Android document-picker launch.
- Persistent library restoration, track names/durations, and paused-player progress.
- Format recognition and time labels.
- Theme-picker selection and persistence across activity recreation.
- Readable text/button contrast across all 12 palettes, plus stable preference IDs.
- Four-color Game Boy scenery.
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
- Corrupt SPC and RSN rejected.
- Oversized extraction rejected and partial files cleaned up.
- Archive paths could not escape the destination directory.

These samples remain outside the repository and APK.

## Screenshots and remaining device checks

`docs/screenshots/` contains native-graphics Robolectric renders of the real Java
view hierarchy. The soundtrack images use a seven-track metadata fixture; the
paused player and theme images use a service-state fixture at 1:13. All 12
theme renders retain the selected track and progress and omit the removed footer. These are not emulator
or physical-device captures, and do not establish Android speaker output.

No emulator or physical Android device was used. A device listening pass remains
for audio routing, Bluetooth, interruptions, notification/lock-screen controls,
and background behavior. The additional GME console formats are compiled in but
do not have sample-based functional validation in this run.

The included APK is debug-signed for sideloading; Play Store release signing and
publication have not been performed. A GitHub Actions workflow is supplied for
future builds; local checks do not prove a remote Actions run succeeded.
