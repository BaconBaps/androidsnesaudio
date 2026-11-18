# Validation evidence

Android/UI checks updated in the managed Linux cloud environment on 25 October
2026 (Europe/London). PSF and replacement RSN native checks were run on
24 October; earlier checks are dated where applicable.

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

One hundred and eight JUnit/Robolectric tests, zero failures and zero skipped:

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
- ChipAmp9x launcher label and accessible sound-chip logo; flat outer player background.
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

## PSF1 and RSN integration (24 October 2026, v1.13.0)

- Android build, 59 JUnit/Robolectric tests and lint pass. New tests cover PSF
  dependency paths, case collisions, rollback, hidden library files and platform aliases.
- `tests/PsfNativeSmoke.java` runs the actual JNI/core with original generated
  MIPS/SPU fixtures: standalone PSF, minipsf with psflib, and UTF-8 metadata.
  Sustained audible PCM, arbitrary forward/backward seeks, fade/end, continuous
  looping and WAV export pass. Two simultaneously open instances produce
  sample-identical audio at the same position; exporting preserves playback state.
- PSF ZIP import resolves case-insensitive dependency names and hides psflib
  entries. Missing libraries, dependency cycles, escaping references and invalid
  compressed data are rejected. Fixtures are generated by `tests/make-psf-fixtures.py`.
- The actual replacement Java/JNI/unarr importer extracts all 36 SPC tracks from
  the supplied solid SF2T RSN. Each extracted track matches the previous Junrar
  baseline by SHA-256. Corrupt input rolls back. Six upstream RAR4 fixtures pass
  native CRC checks: stored, default, maximum, delta filter, PPMd and audio filter.
- Existing native regression checks pass for all 65 supplied SPC tracks, ZIP/RSN
  extraction, VGZ seeking with Loop off/on, and independent SPC/VGZ WAV export.
- New libraries are built for arm64-v8a and x86_64. The APK is debug-signed for
  updating the previous test build. No proprietary BIOS or sample game music is bundled.

These are host native tests and Android Robolectric tests, not physical-device
listening tests. Real-game PSF compatibility, Android speaker output and background
behavior require device verification. That v1.13.0 build excludes PSF2 and MIDI;
MIDI support is added in v1.14.0 below.

## MIDI and SoundFonts (24 October 2026, v1.14.0)

- Android build, 65 JUnit/Robolectric tests and lint pass. Added tests cover default
  bank and per-track preference restoration, copied imports, rename/delete,
  fallback after deletion, rejected import rollback, document-picker launch and
  the MIDI track bank-choice menu and service-level position/paused-state
  restoration when changing banks. UI renders use native-graphics Robolectric.
- `tests/MidiNativeSmoke.java` runs actual JNI/TinySoundFont with original type 0
  and type 1 fixtures, tempo changes, running status, program changes, sustained
  notes and percussion. It checks audible PCM, arbitrary forward/backward seeks,
  looping, straight playback end, independent WAV export, changing banks and
  independent decoder state. Both the bundled Pixel GM and an external TimGM6mb
  bank pass. TimGM is a local test input only; it is not distributed here.
- Invalid running status, truncated MIDI, unsupported SMPTE timing, invalid SF2
  headers and out-of-range table indexes are rejected. SF2 RIFF sizes, tables,
  index ordering, instrument/sample references and preset expansion are bounded
  before synthesis. Banks are limited to 64 MiB and MIDI data to 16 MiB.
- The optional original MIDI demo is rendered through its complete 16-second
  score and two-second tail. Regenerate it with `scripts/generate-midi-demo.py`.
- Existing SPC/RSN, VGZ and PSF native regressions are rerun by
  `scripts/test-samples.sh`; no supplied game music or external SoundFont is bundled.

Pixel GM intentionally uses simple synthetic tones; these checks do not establish
accurate acoustic instruments, compatibility with every game-specific bank or
vendor-specific MIDI extension. Physical-device listening, routing, bank-switch
latency and large-bank memory use remain device checks.

## One-shot SoundFont compatibility (24 October 2026, v1.14.1)

Reproduced the reported rejection with the supplied `illusion_of_gaia.sf2`.
Three non-looping regions (Timpani, Hit, Gong) have unused loop markers that
TinySoundFont converts to an end one sample before the start. The importer
incorrectly required ordered loop markers for every region, even when unused.
The supplied `F-Zero.sf2` did not contain regions triggering this check.

Validation now follows the synthesizer's actual loop condition: only enabled
loops with start before end require loop bounds checks. Sample playback bounds
remain checked for every region. The native smoke test generates this exact
one-shot pattern and verifies import and audible playback. Separate invalid
sample offsets and genuinely active out-of-bounds loops remain rejected.

`tests/SoundFontBankSmoke.java` validates both supplied banks and renders an
original generated MIDI sequence selecting all 128 GM programs. Both banks
produce audible PCM and reach the end. These are host/JNI tests, not a device
listening pass; no claim of exact game instrument mapping is made. Supplied
SoundFonts stay outside the repository and APK. Android build, all 65 tests and
lint pass for the updated APK.

## Import review, queue and MIDI titles (24 October 2026, v1.15.0)

- Android build, 79 JUnit/Robolectric tests and lint pass. New provider tests
  exercise granted tree URIs, root folder names, nested paths, music/dependency
  filtering, repeated directories, invalid provider names and scan limits.
- Review UI tests cover disabled empty actions, both Android picker intents,
  multiple files, Cancel without copying, selection retention across rotation,
  and separate Add/Add-and-queue results. Library import tests use a decoder
  shadow for metadata; these do not establish native/device audio output.
- Batch tests verify PSF folder/dependency paths and rollback after a later file
  fails, including keeping original source files. Existing native sample tests
  remain in `scripts/test-samples.sh` for ZIP/RSN, SPC, VGZ, PSF and MIDI regressions.
- Service tests use a decoder and full-audio-sink shadow: append/reorder/removing
  non-current entries keeps the decoder and position, removing the playing entry
  advances, and an empty or paused queue does not start playback on append/removal.
- Activity tests check Add leaves the queue alone, Add and queue refreshes the
  library and appends without replacing current playback, queue actions adjust
  the active index, and explicit MIDI bank titles scroll and follow bank renames.
  Default-bank tracks and non-MIDI titles remain unmodified.
- `import-music.png` and `playback-queue.png` are native-graphics Robolectric
  captures of the real views with generated files/metadata and synthetic FFT
  input, rather than physical-device captures or real game recordings.

Physical Android document-provider grants, cloud/offline providers and device
background behavior still need device verification. No additional storage or
network permission was added. The APK retains the existing package and signing
certificate for in-place updates.

## Library actions and controls (24 October 2026, v1.15.1)

Android build, 81 JUnit/Robolectric tests and lint pass. The Library delete test
now verifies that inline Delete buttons are absent, child-row long press opens
an action menu, Delete has a visible separation from Add to queue, and deletion
still requires confirmation. A new test verifies Add to queue preserves the
current track/position while Play selects the soundtrack. Group headings do not
open the soundtrack action menu. A narrow-screen layout check verifies all five
playback controls retain at least 48 dp touch areas, visible gaps and fit within
a 320 dp screen. The updated screenshots remain native-graphics Robolectric
renders; device touch/long-press behavior remains for physical-device testing.

## Pixel cog and themed menu (24 October 2026, v1.16.0)

Android build, 82 JUnit/Robolectric tests and lint pass. Menu tests navigate music
shortcuts, Playback tools and Settings, verify unavailable export/MIDI actions
are disabled, return to the parent menu and exercise the queue shortcut. Existing
theme selection, SoundFont management, favourites and Exit tests now navigate the
actual themed dialogs instead of Android PopupMenu. The header check verifies
that the cog has no background or image tint. Screenshots of the main menu and
both submenus are native-graphics Robolectric renders in Llama; existing light
and dark palette screenshots show the new cog. The vector is original MIT app
artwork, with merged pixel runs and short paths; no external logo art was copied.
Physical-device navigation and touches remain device checks.

## MIDI filenames, grey cog and rename (24 October 2026, v1.16.1)

Android build, 85 JUnit/Robolectric tests and lint pass. The import test verifies
original filename titles for individual uppercase `.MID` and nested/archive
`.mid`/`.midi` entries despite conflicting decoder metadata; non-MIDI metadata
and persisted titles remain unchanged. Rename tests verify a saved, trimmed label,
independence of duplicate imports, rejection of blank/overlong names, and unchanged
track paths, files, favourites and SoundFont preferences. The UI test opens Rename
from the long-press menu, keeps the dialog open on invalid input, saves the new
label and refreshes the main header. Existing menu/action screenshots now include
Rename with Delete still separated. The cog generator uses only neutral greys.
Previously imported MIDI titles are retained until re-import, because legacy
records do not retain original filenames for all archive entries.

## Interactive custom themes (24 October 2026, v1.17.0)

Android build, 91 JUnit/Robolectric tests and lint pass. New persistence tests
cover named palettes, independent player/library/spectrum roles, updating an
existing profile, independent duplicates/imports, serialization round trips,
invalid versions/base names/roles/alpha colours, file-size limits and fallback
when a selected custom ID is absent.

UI tests tap the actual preview overlay and verify that a Library-button tap
opens colour tools rather than importing music. They exercise swatches, popup
Undo preserving earlier edits, spectrum colours, snapshot stability while the
service advances, draft retention across recreation, and Cancel restoring the
live player without saving. Save tests name a palette and verify restoration in
the main window, import screen and widget heading. Export/import tests use the
real document-result paths with local provider streams and verify an independent
loaded copy. Existing built-in palette and playback/UI tests remain passing.

`theme-editor.png`, `theme-colour-picker.png` and `saved-themes.png` are
native-graphics Robolectric renders of the actual controls and frozen metadata/
example-spectrum fixtures. Library screenshots show the new pixel heading.
These are not physical-device captures. Device touch/colour-drag responsiveness,
provider behavior and launcher rendering remain device checks. No native audio
engine, library schema or storage permission was changed by this feature.

## Dot matrix heading (v1.17.1)

The playlist/game heading uses the bundled OFL-licensed Doto variable font,
with rounded dots and weight 800. Native-graphics Robolectric screenshots
were reviewed for heading readability; all 91 tests and Android lint pass.

## Single-line dot matrix titles (v1.17.2)

Game/playlist headings and Now Playing titles use the same single-line
BouncingTextView, which pauses for 1.2 seconds at either edge and uses a cosine
ease in/out in both directions. Short titles remain still; repeated playback
updates do not restart motion. Now Playing uses Doto weight 900 and retains
the theme accent colour. UI assertions verify both titles are single line
and the Now Playing font remains bold; motion checks verify easing symmetry.

## Overflow rendering correction (v1.17.3)

The shared BouncingTextView now draws the complete text Layout behind a fixed
viewport clip. Moving TextView.onDraw previously moved an internally clipped
viewport, leaving the original offscreen glyphs invisible. The correction
applies to playlist/game headings, Now Playing and tracklist entries.
A native-graphics regression test starts with an empty visible prefix and
verifies the previously offscreen suffix produces visible pixels at the far
edge; timing/easing, unchanged-title and short-title checks remain in place.

## Nintendo 64 and editor hit regions (v1.18.0)

- Both ARM64 and x86_64 APKs compile with the portable LazyUSF2 interpreter and
  low-level RSP. Native USF fixture checks cover six playable variants,
  sustained audio, seek, fade, loop, independent WAV export, concurrent states,
  ZIP libraries, Unicode/CP1252 tags, malformed data and post-error recovery.
- AddressSanitizer plus leak detection pass valid render/reset/seek and invalid
  container/state/dependency/DMA/no-audio checks; see USF_SUPPORT.md.
- SPC/RSN archive and 65-track native smoke checks, SPC/VGZ seek/export checks,
  PlayStation PSF and MIDI native tests pass with the combined engine.
- Theme editor exposes only the first tracklist row, clips all target bounds
  through ancestor viewports, and preserves frozen targets after recreation.
  A 30-row UI test verifies hidden rows cannot intercept Now Playing edits;
  screenshot: screenshots/theme-editor-tracklist.png.
- 96 JUnit/Robolectric tests pass; Android lint reports no issues. Real device
  USF playback, performance and long scrubs remain for user/device testing.

## N64 stutter reduction (v1.18.1)

- Both Android compile databases confirm `-O2` for the LazyUSF2 cached CPU and
  RSP. The previous debug APK omitted optimisation.
- The original CPU-heavy N64 fixture benchmark improved from 0.928781 s to
  0.414504 s for 5.01551 s of audio on x86_64, with identical PCM hashes. This
  is a host synthetic result; no phone/game-driver speed claim is implied.
- Audio-priority worker, larger N64 output buffers and nonblocking priming are
  covered by a service test with partial writes, seek, pause/resume and stop.
  A second test verifies queued audio finishes before the next track starts.
- 98 JUnit/Robolectric tests, Android lint, and the complete native SPC/RSN,
  VGZ seek/export, PSF, MIDI and USF regression suite pass.

## ChipAmp9x branding and visualizers (v1.19.0, 25 October 2026)

- Current app branding, accessible header, launcher/widget labels, notification,
  export fallback, MIDI demo, original bank metadata, docs and downloads use
  ChipAmp9x. The approved cream/green/gold wordmark has true transparency; the
  grey settings cog has a thicker black pixel outline. The signing key and
  application ID remain unchanged for installation/data compatibility. Original
  copyright-holder names remain in their legal notices.
- Tap cycles Spectrum, stereo Waveform, Stereo Scope and radial Orbit; hold
  resets Spectrum. Selection persists through recreation and theme editing.
  UI tests capture all four styles and verify cycling, persistence and frozen
  editing. Waveform extraction tests cover silence, right-only audio, opposite
  phase and unchanged input PCM. Traces use the stronger channel for triggering
  and preserve independent left/right samples from decoded audio.
- 101 JUnit/Robolectric tests and lint pass. The full native SPC/RSN, VGZ, PSF,
  MIDI and USF regression suite passes with the renamed demo and regenerated
  original SoundFont metadata. Device tap/animation behaviour remains for user
  testing. No microphone or additional permission is required.

## Saved playlists (v1.20.0)

- Ordered path/subsong references are saved atomically in private playlists.json,
  separately from the imported library. Persistence tests cover repeated tracks,
  subsongs, metadata resolution, rename/replace/delete, missing sources, name
  validation, and rejected writes leaving existing data untouched.
- A mixed-game UI test saves the queue through the menu, browses the Playlists
  accordion, reloads after activity recreation, appends without interrupting
  playback, renames and deletes without removing music. Repeated tracks show
  only the active queue occurrence. UI tests also cover overwrite confirmation,
  cancel, and the empty-library import/save controls.
- Existing platform accordion expansion/selection remains unchanged; Playlists
  is an additional section. Screenshot: screenshots/saved-playlists.png.
- 108 JUnit/Robolectric tests and Android lint pass. No decoder, permission or
  audio format changes are involved. Device keyboard/dialog interaction still
  needs user/device testing.

## PSF decimal-comma timing compatibility (v1.20.1)

- The supplied Resident Evil 3 collection contains 133 self-contained PSF files
  with decimal-comma lengths, including `0:6,052078` and much longer fractional
  representations. The old std::stod-based parser rejected them.
- A shared locale-independent fixed-point xSF timing reader accepts seconds,
  MM:SS and HH:MM:SS, decimal dot/comma, whitespace and fractional rounding.
  Missing, malformed or out-of-range optional timing uses the existing safe
  defaults instead of rejecting otherwise valid music. Fade remains capped at
  one minute (invalid/excessive values default to five seconds). Body/version,
  checksum, memory and dependency validation are unchanged.
- All 133 supplied tracks opened through the app JNI; exact durations matched
  an independent Python Decimal calculation. All reopened after app archive
  extraction. 131 produced audible PCM during the first 557 ms; this initial
  window does not establish that the other two are silent throughout.
- Native timing tests cover the supplied formats and invalid/fallback cases.
  Original generated PSF fixtures verify import/playback for comma, whitespace,
  unknown and oversized timing values; USF uses the same parser. The full
  native SPC/RSN, VGZ, PSF, MIDI and USF suite, 108 Android unit/UI tests and lint
  pass. No supplied game audio or collection files are included in the APK or
  repository. The uploaded 7z was unpacked only for local verification; app
  import remains files/folders, ZIP and supported RAR/RSN archives.

## Reactor skin (v1.21.0)

- 109 JUnit/Robolectric tests pass with zero failures, errors or skipped tests;
  Android lint reports no issues. Debug APK builds for arm64-v8a and x86_64.
- Native-graphics renders cover the actual Reactor player, live spectrum fixture,
  stereo waveform fixture and platform-accordion library. These are Robolectric
  captures, not physical-device screenshots.
- All eleven palettes meet the existing text/selection/playback contrast checks;
  texture contrast checks now use each palette's actual skin, including Reactor.
- Reactor's six raster frame resources compile as Android nine-patches. Corner
  pixels stay fixed as the frame grows. Pressed button frames differ from the
  original skin. Saved/exported/imported custom Reactor themes retain their
  mechanical base and spectrum defaults while accepting colour overrides.
- Existing theme-switching tests preserve current track and playback position,
  and now include Reactor. No decoder, importer or audio-thread changes.
- APK signature matches the previous release; 16 KiB zip alignment passes.

## Reactor nine-patch and grid labels (v1.21.1)

Removed decorative hatching and button dots from stretchable frame bands, fixing
stretched copper bars on the outer casing/player frame and dark smears on buttons.
The existing native-graphics frame regression now checks horizontal uniformity
across wide Reactor chassis and button bands, alongside fixed-corner checks.
Theme preview names use the same pixel typeface and 7 sp size as NOW PLAYING,
drawn independently of thumbnail scaling and wrapped to two lines when needed.
Updated native-graphics captures cover the player, library and theme grids.
All 109 tests pass with no failures, errors or skips; lint reports no issues.
The APK signature matches v1.21.0 and 16 KiB ZIP alignment passes.

## Shared button/panel edging and borderless Reactor (v1.21.2)

The main app background uses a separate borderless surface, preserving Reactor's
metal texture without its outer casing frame. Raised/pressed button edges and
section-panel edges now share Reactor's inset detail across palettes; the existing
light-surface shadow softening remains active. Native-graphics captures refresh
the theme gallery, theme grids, player and library.
All 109 tests pass with no failures, errors or skips; Android lint reports no
issues. APK signing certificate matches v1.21.1; 16 KiB ZIP alignment passes.

## UI fonts and channel mixer (v1.22.0)

Bundled Pixelify Sans and Jersey 15 are Android font resources with OFL notices.
Native-graphics UI captures exercise themed mixer rows and settings headings.
UI checks verify the selected font families, menu support gating, selected mute
and solo states, and closing the mixer when the current track changes.

Mixer state is published as an immutable snapshot. Only the audio worker calls
the GME voice API; queued actions verify the exact current track before applying.
Service tests cover mute/solo restoration, seek preservation, reset and stale
actions across track changes. A generated original VGM PSG tone exercises real
JNI channel names, individual mute and solo, all-muted playback for more than
nine seconds, seeking while muted, audible reset and independent decoder state.
Silence detection is suppressed while muting so an intentional silent mix does
not trigger early track advancement. Normal loop/silence behaviour returns on reset.

Release checks: 113 JUnit/Robolectric tests pass with zero failures, errors or
skipped tests; Android lint reports no issues. The full native sample script
passes for SPC/RSN, VGM/VGZ, PSF, MIDI and USF, including mixer and independent
export checks. APK signature matches the previous build and 16 KiB ZIP alignment
passes. Screenshots are native-graphics Robolectric renders, not phone captures.

## Contextual track tools (v1.22.1)

Long-press menus now expose export and playback tools across tracklists, queues
and library collections, with MIDI-only SoundFont actions. A UI regression
checks that tools/export target a selected non-playing track and that choosing
a track from a saved playlist exports the chosen entry without changing the
playing track or position. Existing MIDI bank-selection and queue-action checks
remain in the suite. Library action contents scroll on smaller screens.
All 114 tests pass with zero failures, errors or skips; Android lint reports no
issues. APK signing certificate matches the previous release and 16 KiB ZIP
alignment passes. No decoder or import-format changes are included in this release.

## PlayStation XA and VAG (v1.23.0)

116 JUnit/Robolectric tests pass with no failures, errors or skips; Android lint
reports no issues. Existing SPC/RSN, VGZ, PSF, MIDI, USF and mixer native checks
pass alongside the new PSX stream checks. The additional importer tests verify
case-insensitive extensions and the scoped 128 MiB per-file allowance while
retaining the ordinary 32 MiB and total 256 MiB limits.

The new native suite compares every PCM sample for 11 original reference streams
covering XA 4/8-bit mono/stereo, both sector layouts, RIFF CDXA, Form 1/Form 2 and
interleaved stream/EOF segmentation. Checks include duration, stream selection,
random/backward seeks, explicit VAG loop-start/full-XA repeat positions,
independent WAV export, malformed-file rejection and ZIP extraction/import.
VAG and 4-bit stereo XA PCM are cross-checked against FFmpeg. ASan/UBSan passes
1250 valid/mutated opens, renders, seeks and loops. Details and reproduction
commands are in PSX_AUDIO_SUPPORT.md. No real game XA/VAG collections or physical
Android device were used, so proprietary layout compatibility remains limited
to the documented formats. APK signature matches the previous release; 16 KiB
ZIP alignment passes.

## Live import readout (v1.23.1)

Status callbacks cover selection staging, document copying, archive extraction,
metadata/subsong reading, saving and batch rollback. Byte-copy reporting is
throttled and the UI coalesces updates to the existing single status line rather
than re-rendering selection rows. Callback-free importer overloads remain intact
for existing callers and native sample tools.

Regression tests cover archive/entry counters and filenames, saving and rollback
messages, byte-for-byte copy integrity, and a real import worker blocked at the
decoder stage while the UI readout updates and survives activity recreation.
The import-progress screenshot is a native-graphics Robolectric capture.
Release checks: 119 tests pass with no failures, errors or skips; lint reports
no issues. The full native sample script passes, including SPC/RSN extraction,
VGZ seeking, PSF, MIDI, USF, the mixer and XA/VAG reference streams. APK signing
certificate matches the previous release and 16 KiB ZIP alignment passes.

## Phosphor and dot-matrix header (v1.24.0)

121 JUnit/Robolectric tests pass with no failures, errors or skips; Android lint
reports no issues. All twelve palettes pass the existing readability checks and
new header foreground/background contrast checks. Header UI assertions cover
the shared recessed surface and accessible 48 dp menu target. The Phosphor
regression checks theme switching without changing playback, rounded dot-control
selection and custom-theme JSON persistence through activity recreation. Raster
checks verify transparent rounded corners, opaque key faces and distinct pressed
states. Native-graphics captures refresh the app gallery and include Phosphor
and a custom blue variant. Gallery dialog captures run last to avoid retained
Robolectric window focus hiding elements in later captures.

The layout and buttons remain live Android views, not a screenshot skin. The
new plastic texture is sampled down once and tiled; rounded bevel geometry has
fixed edge widths. Header text uses the same bundled Doto family as existing
display titles. No decoder or playback-thread changes. APK certificate matches
the previous release and 16 KiB ZIP alignment passes. These are Robolectric
captures/checks; no physical-device rendering or performance claim is made.

## Hi-fi header and Phosphor mode icons (v1.24.1)

123 tests pass with zero failures, errors or skips; Android lint reports no issues.
Header motion tests cover the initial hold, smooth exit, offscreen wrap, smooth
return, cycle reset and invalid/zero-size inputs. The actual header uses this
motion only for visible, focused windows with Android animation enabled and no
frozen theme editing. Native-graphics app/gallery captures use complete static
header text. The smaller title and exact subtitle replace the previous chip mark;
Phosphor's mode glyphs now use explicit crossed-arrow and repeat-arrow paths.
These captures are Robolectric renders, not a physical-device animation check.
APK signing certificate matches the previous release; 16 KiB ZIP alignment passes.

## Randomized pinball header (v1.25.0)

127 tests pass with no failures, errors or skips; Android lint reports no issues.
The pure animation model checks stage boundaries, smooth interpolation, seeded
repeatability, changing cycle parameters, bounded/non-overlapping glitches and
independently varying waveform speed/frequency/direction. Sampled wave points
remain finite and within the display grid. A native-graphics test renders title,
glitch, wave and reform phases, checks that they differ and that the wave has
visible pixels in the selected foreground colour; its actual-renderer contact
sheet is header-pinball-sequence.png.

UI tests activate the header frame, title and cog independently and verify that
each opens Settings, then verify that Back reaches the full app menu. Existing
menu-action tests use that navigation. The raster title mask is cached, with
choices made once per cycle and no GIF/video asset in the APK. Animation only
runs in visible/focused windows with system animation enabled and no frozen
editing; other renders show the static title. No audio-worker or decoder changes.
APK certificate matches the previous release; 16 KiB ZIP alignment passes.
These remain Robolectric checks, not physical-device animation benchmarks.

## v1.26.0 — styles, shared palettes and calm header

Full debug build, Robolectric suite and Android lint pass. Classic and ChipAmp5
use the same ten original palettes; a regression test compares every colour role
in app, player and library. Style-specific selections/custom themes are tested.
Reactor resources are removed, Classic header corners are square, and ChipAmp5
keeps rounded plastic geometry. Tiny5 is bundled under its OFL licence.
Header tests cover quiet holds, bounded glitches, downward exit and sequential
letter returns. Screenshots use Robolectric native graphics, not a physical
Android device. APK signature matches the preceding release and 16 KiB zip
alignment passes.

## v1.26.1 — restore typography

Reverted the v1.26.0 Tiny5 substitutions and size multipliers. Pixelify Sans body
text, Press Start 2P pixel labels/visualizer, Doto titles and Jersey 15 menu
headings retain their preceding assignments. The two styles, identical palettes
and quieter header choreography are retained. Debug build, Robolectric tests
and lint are checked; APK signing continuity and 16 KiB alignment are verified.

## v1.27.0 — Chakra Petch and independent header colours

Regular UI text uses Chakra Petch, with bold weight bundled; pixel labels, Doto
track titles, Jersey 15 headings and header lettering remain unchanged. Header
dots/cog and screen have independent editable colour roles with unchanged
fallbacks. Theme JSON round-trip and editor registration tests cover the new
roles. Debug build, Robolectric suite and lint pass. Screenshots are generated
with Robolectric native graphics; no physical-device verification was performed.
APK signing continuity and 16 KiB zip alignment pass.

## v1.27.1 — Player Menu header and compact mode controls

All header regions open Player Menu, with Settings reachable from its entry.
The existing header UI test checks the title, cog and display surround.
ChipAmp5 shuffle/loop buttons use narrower layout weights, Previous/Next height
and smaller glyphs while retaining ON/OFF state labels. Classic is unchanged.
Debug build, Robolectric suite and lint pass; renders use native Robolectric
graphics, not a physical device. APK signature continuity and 16 KiB alignment
are verified.

## v1.27.2 — mode lamps and Favourites in Playlists

ChipAmp5 shuffle/loop glyphs are centred and use a recessed corner indicator
instead of state labels; Classic retains ON/OFF text. Accessibility descriptions
retain the state. Library Playlists includes the live, non-deletable Favourites
entry, available when empty. A UI regression test checks opening it, removing a
favourite and keeping playback unchanged. The full debug build, Robolectric suite
and lint pass. Renders use Robolectric native graphics; physical-device testing
has not been performed. APK signing continuity and 16 KiB alignment pass.

## v1.28.0 — expanding dot-matrix header menu

Full debug build, Robolectric suite and Android lint pass. Header menu regression
checks cover exact anchor width, no backdrop dimming, height bounded above the
deck, in-place submenu reuse, Back/Close dismissal and unchanged deck position.
Settings title and entries use the same smaller dot-matrix size. Composite UI
renders show the menu anchored over the real activity. Tests use native
Robolectric graphics; no physical-device animation verification was available.
APK signing continuity and 16 KiB alignment pass.

## v1.28.1 — restore Settings sizes and style-aware widgets

Settings returns to the same type-size hierarchy as the other matrix menu pages.
The launcher widget uses current PlayerSkin frame/readout/button geometry and
palette, including rounded ChipAmp5 controls and custom Track readout colour.
Skin images are cached and resize-aware; text, chronometer and click actions
remain native RemoteViews. Tests check both style renders, empty-state updates
without starting playback and unchanged foreground-service playback actions.
Full debug build, Robolectric suite and lint pass. Widget screenshots are native
Robolectric renders, not a physical launcher verification. Signing continuity
and 16 KiB zip alignment pass.

## v1.28.2 — fixed expanded menu display

Every menu page uses the same full expanded height, bounded by available screen
space. Submenu changes no longer animate or resize the display; opening/closing
still animate. The existing UI regression now requires exact height equality
across Player Menu, Settings, Playback and Back navigation. Full debug build,
Robolectric tests and lint pass. APK signing continuity and 16 KiB alignment pass.

## v1.29.0 — Library button expansion

Library now uses the shared expanding-panel container anchored to its button,
without a dimmed backdrop. Its fixed expanded height stops above the playback
deck; accordions scroll internally. Header/collapse, selection, playlist saving,
import and long-press actions retain their behaviours. Panels are mutually
exclusive and are dismissed on Activity destruction. UI tests check button-width
alignment, fixed height after accordion expansion, unchanged playback/deck
position, collapse and persisted accordion selection. Full debug build,
Robolectric suite and lint pass. Renders use native Robolectric graphics, with
no physical-device animation verification. Signing continuity and 16 KiB APK
alignment pass.

## v1.29.1 — taller Library panel

The Library expansion now fills available app space below its button, covering
the playback section and respecting bottom padding/insets. Playback continues
underneath, and collapsing reveals its unchanged controls. Existing UI tests now
check that the panel extends over the playback deck while preserving fixed
height, accordion actions, selection and playback state. Full debug build,
Robolectric tests and lint pass. Signing continuity and 16 KiB alignment pass.

## v1.30.0 — menu cleanup and shared palettes

Player Menu now places Look & Sound under Music and About/Exit in App at the
bottom, removes the Settings/Preferences page, and leaves Save playlist and
Favourites in Library. Menu UI tests cover order, removed entries and retained
Playback navigation. Palettes and all saved/imported custom themes are shared
between Classic and ChipAmp5; switching style retains colours and custom ID.
Legacy saved files remain readable, and existing themes are not deleted.
Regression tests check shared custom listings, style switching/recreation,
colour preservation and unchanged playback. Full debug build, Robolectric suite
and lint pass. APK signing continuity and 16 KiB alignment pass.

## v1.31.0 — NES names and persistent track renaming

Generated original NSF/NSFE fixtures passed through the real host JNI decoder:
NSFE labels, game/composer, durations and playlist ordering are already read
correctly. Standard NSF has game metadata but lacks per-song labels. New unnamed
NES imports use Track 01 etc.; legacy generated NSF names are cleaned up without
overwriting explicit user names. Track renames persist per path/subsong with
atomic save/rollback and leave source files unchanged. UI tests cover validation,
dark-theme readability, queue/current-title refresh, playlist/favourite identity
and unchanged playback position/state. Full debug build, Robolectric suite and
lint pass. Native metadata smoke test, signing continuity and 16 KiB alignment
pass. No physical-device playback verification was performed for this release.

## v1.32.0 — arcade and wider VGM chip support

Pinned libvgm provides the broad device collection for non-GME VGM chips. OPL4's
GPL-2.0-only core is excluded and explicitly rejected. Existing console VGM
routing/mixing remains on GME. Expanded file/ROM sizes and command/data-block
lengths are bounded, GD3 conversion is portable to API 26, and old imports use
the new route without reimporting. User music remains private and uncommitted.

All supplied CPS-1 (47), CPS-2 (72), and Simpsons (45) tracks opened and generated
nonzero PCM within 2 seconds: 164/164. Representative tracks pass forward/backward
seeks, native loop-boundary crossings, loop-off fades/end and byte-identical
raw-VGM/VGZ rendering. CPS-2 WAV export is audible at zero/nonzero offsets and
does not advance live playback. Generated tests reject unknown commands,
truncated sample blocks, invalid loop offsets and OPL4, and verify Unicode GD3 and decoder recovery.

SPC/RSN extraction and 65 real SPC tracks, GME seek/mixer/NSFE checks, PSF, USF,
MIDI and 11 XA/VAG streams pass against the combined native library. Android
ARM64/x86_64 builds, all 138 Robolectric tests and lint pass. Both ABI configs
exclude OPL4; ARM64 symbols confirm its emulator is not linked. Signing continuity
and 16 KiB APK alignment pass. Host JNI proves generated audio; physical-device
playback quality/performance still needs testing.

## v1.33.0 — add individual tracks to playlists

Track and queue long-press menus offer Add to playlist, with an existing-playlist
picker and New playlist flow. Append writes one reference atomically, preserves
order/unavailable references, distinguishes subsong indexes, avoids accidental
duplicate additions and enforces the 1000-track limit. Creating a playlist does
not overwrite an existing name. UI tests cover cancellation, existing/new targets,
name validation, duplicate handling and unchanged playback queue/position/state.
Full debug build, Robolectric suite and lint pass. Native decoding is unchanged.
APK signing continuity and 16 KiB alignment pass.
