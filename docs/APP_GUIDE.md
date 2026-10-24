# ChipPlayer 199x

A small Android player for big retro soundtracks. A live pixel spectrum,
ten retro palettes, and a local library for the games you grew up with.

<img src="screenshots/theme-desktop-98.png" width="300" alt="ChipPlayer 199x soundtrack library and player with live spectrum" />

## Install without Android Studio

Download [ChipPlayer-199x-debug.apk](../downloads/ChipPlayer-199x-debug.apk) to your Android device,
open it, and allow installation from your browser or file manager when prompted.
Requires **Android 8.0 or later and a 64-bit ARM device** (x86-64 is also included
for emulators). This is a debug-signed development build, not a Play Store release.

The app was previously named PixelSPC; installing this update keeps your library,
favourites, and saved theme.

Tap **⋮ → Import music**, or **Library → Import music**, and select your
file using Android's file picker. Tap a track to play. Use the fixed player panel
for pause, previous/next, seeking, shuffle, and continuous looping. The outer
buttons show ON/OFF states: left is Shuffle (changes the next track), right is
Loop (lets the current sound program keep running). These two controls are
borderless and show ON/OFF using text and accent color. Both preferences are saved.
Long track titles scroll smoothly to the end and back, pausing at each edge; this
works in the track list and Now Playing. The seek bar is inset from the panel edges.
VGZ/VGM seeking emulates the intervening chip audio, so long jumps can take a
moment. The time display shows **SEEKING → target** during this silent catch-up,
then audio resumes automatically if it was playing. Seeking while paused keeps
the player paused. Seeking runs in small steps: pause, another seek, or selecting another
track can interrupt it with Loop either on or off.
Playback continues in the background with notification and lock-screen controls. Headphone disconnect
pauses playback. Music is copied into the app's private storage and survives app
restarts; uninstalling removes imported files. Keep your original music files.

Open **Library** and tap **Delete** beside a soundtrack to remove that import and
its private music files after confirmation. Duplicate imports are separate entries;
your original files are kept. Deleting the queued soundtrack stops its playback.
The tracklist only shows a centered Import music button when empty. The larger
Library browser groups imports into collapsible platform sections, sorted by
platform and soundtrack name. It remembers which groups you expand or collapse.
Known metadata aliases (such as SNES and Super Famicom) share one group; mixed
archives appear under Multiple systems. Expand a platform to select or delete
a soundtrack.

The game title, metadata, Tracklist header, and outer tracklist edging stay fixed
while tracks scroll. The bottom border remains visible at every scroll position.
Theme selection and Help also live in the **⋮** menu. The four light palettes use
softer frame shadows, while the playback deck keeps its dark finish.

Choose **⋮ → Exit app** or the notification’s pixel **× Exit app** action to stop playback, release the audio device, remove the
notification, and close the app’s task. Normal Home/Back navigation continues to
allow background playback. Android may cache the idle process after Exit, but the
playback service is stopped. Notifications use Android’s standard background with no custom color or artwork. Android controls the media notification layout; newer
versions expose Exit as a media-session action, and older compact notifications
show Play/Pause, Next, and Exit (Previous remains available when expanded).

No music is bundled. Import your own files, including the example sets supplied
for development. The app requests no Internet or broad storage permission.

## Export, favourites, and sleep timer

Long-press a track, or choose **⋮ → Export current track…**, to save audio as a
WAV file. Choose a start time and duration in `minutes:seconds`, optionally add a
short fade in/out, then choose the destination with Android’s Save picker. The
full-track shortcut uses the track’s tagged duration (or its default duration).
Exports are stereo 44.1 kHz, 16-bit PCM WAV, up to 10 minutes long with start
offsets up to 60 minutes. A 30-second clip uses about 5 MB. Rendering uses a
separate decoder, so playback can continue. Cancel stops preparation and attempts
to remove the empty destination; providers that refuse deletion may leave a file
to remove manually. No phone sound settings or extra permissions are needed.

Tap **☆** beside a track to favourite it. **⋮ → Show favourites** collects starred
tracks across games; tap one to play the favourites queue. Stars survive app
restarts. Duplicate imports have independent favourites, and deleting an import
clears its stars. Choose a game from Library, or **⋮ → Back to soundtrack**, to
leave the favourites view.

**⋮ → Sleep timer** offers Off, 15, 30, 60, or 90 minutes. Playback pauses when
the timer expires, including with Loop on, keeping the track and position for
later. The menu shows the remaining minutes. The timer continues while using
other apps; Exit cancels it.

## Home-screen widget

Long-press your Android home screen, open **Widgets**, and add
**ChipPlayer 199x · Now Playing**. The resizable widget follows your selected
palette, with the player’s original nine-patch edging, title, artist/game,
elapsed clock and Previous / Play-Pause / Next controls. Tap the heading or track
name to open the app. When there is no loaded track, Play opens the app so you
can choose one. Exit clears the widget’s playback state.

The launcher runs the elapsed clock itself, without periodic background polling.
The live spectrum remains in the app. The widget controls the same queue,
shuffle and loop settings as the player; it does not start a separate decoder.

<img src="screenshots/homescreen-widget.png" width="360" alt="ChipPlayer 199x Now Playing home-screen widget" />

The header uses the selected green sound-chip logo, with a transparent background
and outlined lettering for light and dark palettes.

## Image-based player skin

The tracklist and platform accordion use dimmed, untextured surfaces, with
contrast-adjusted text. The Library button is slightly lighter, and section
labels are vertically centered with their rails.

The Library button and playlist sit above a separate, darker Now Playing deck.
Its spectrum sits above the track details and transport controls; the darker
palette adapts each theme’s accent to keep text readable. Header actions are collected in the single ⋮ menu.

Original pixel PNGs give every theme raised metal borders, corner screws, inset
displays, metallic section rails, pressed controls, and a ridged seek thumb. Six
Android nine-patch images stretch the frames without stretching their corners.
Grain tiles independently; dark skins add subtle screen scanlines, while light
skins use quieter surfaces to preserve text contrast. The controls remain real
Android views with accessible labels, and shuffle/loop stay borderless.

Choose **Llama** at the top of the theme grid for midnight metal, phosphor
green and brass details inspired by late-90s desktop players. Existing theme
choices are preserved. [Skin assets and regeneration](SKIN.md).

## Themes

Choose **⋮ → Choose color theme** to choose a palette
from a grid of miniature player previews. The selected card has a checkmark. The choice is saved
between launches and updates the controls, dialogs, system-bar icons, and pixel
spectrum without interrupting playback. Choose Pixel Light, Pixel Dark, or one of
seven console-inspired palettes: SNES, Super Famicom, NES, Famicom, Game Boy,
Mega Drive / Genesis, and GameCube. Llama adds a classic PC look.
Six palettes use darker backgrounds; four retain light hardware-inspired
colors. Pixel Dark uses teal, Super Famicom navy/red, NES graphite, and GameCube rich violet. The spectrum uses each
theme’s accent and secondary colors.

[Browse all 10 theme screenshots](THEMES.md).

## Formats

| Files | Supported content |
| --- | --- |
| `.mid`, `.midi` | Standard MIDI type 0/1 with PPQ timing; built-in Pixel GM or imported `.sf2` banks |
| `.psf`, `.minipsf` | Original PlayStation soundtracks; import shared `.psflib` files together in a ZIP |
| `.spc` | Super Nintendo tracks, including ID666 metadata |
| `.rsn`, `.rar` | RAR-based SPC sets (including solid RAR4 archives) |
| `.zip` | Sets containing supported tracks, including subfolders |
| `.nsf`, `.nsfe` | NES; each subsong becomes a selectable track |
| `.gbs` | Game Boy |
| `.vgm`, `.vgz`, `.gym` | Logged console/chip music supported by GME |
| `.hes`, `.kss`, `.ay`, `.sap` | Additional Game Music Emu formats |

ROMs (`.sfc`, `.smc`, etc.) are not audio files and are not supported. Encrypted
archives and RAR5 sets are not supported by the unarr RAR reader. Import limits:
128 MB input, 32 MB per extracted file, 256 MB total extraction, 1,000 songs.
Malformed or unsupported tracks roll back the entire import. Archive paths are
flattened so entries cannot write outside the library.

Track duration uses embedded timing when available, or two loops where tagged;
otherwise it defaults to 3 minutes. GME adds its ending fade. Auto-next wraps the
current set. With Loop enabled, the SPC emulator keeps executing the original
sound program: no timed fade, seek to zero, or audio-device restart occurs at the
musical loop boundary. This preserves natural SPC loops, rather than replaying
the introduction. One-shot tracks without a programmed loop can still become
silent. Loop cannot invent missing musical loop points.

An SPC with an explicit **xid6 loop-length tag (0x31)** also gets a **STRAIGHT /
LOOP** button in its track row. This saved per-track choice overrides the default
loop setting when the track starts. Straight uses its ordinary tagged duration
(or the fallback duration); Loop plays continuously. A total-duration or intro
length tag alone does not establish a loop, so untagged tracks only have the
bottom-right manual control. Existing imported SPC files are checked for tags
when an older library is loaded.

The graphic is a **24-band, logarithmic pixel spectrum** calculated from the actual
44.1 kHz stereo PCM using a Hann-windowed FFT. It handles either stereo channel,
including opposite-phase signals. Pixel peaks fall after sounds stop; pausing
settles the display. It never simulates activity when audio is silent and needs
no microphone or system-audio-capture permission.

There is no playlist editor, individual-song deletion, or configurable default duration yet.

## PlayStation PSF sets

Import a ZIP containing the `.psf` / `.minipsf` tracks and every referenced
`.psflib`, retaining the original folders. A self-contained `.psf` also imports
on its own. A miniPSF selected by itself cannot access sibling library documents;
use a complete ZIP set. Libraries are retained privately but hidden from the
track list. Metadata groups these soundtracks under PlayStation.

Original PlayStation **PSF version 1** is supported. PSF2, PSP and other xSF
variants are not included. No BIOS is needed. PSF length/fade tags determine the
normal ending; absent length defaults to three minutes plus fade. Loop keeps the
emulated program running, and export uses an independent decoder. Long seeks
emulate the intervening audio, so catching up can take a moment.

This test build has native regression coverage with original generated MIPS/SPU
fixtures; compatibility with real game PSF drivers needs device/user testing.
The combined APK includes GPL-covered PSF code and is distributed under GPLv3;
the app's original source and artwork retain their MIT grant. See THIRD_PARTY.md
and COPYING-GPL-3 at the repository root for distribution terms and source.

## Build

No Android Studio is required. Install JDK 17 or 21 and Android command-line tools,
set `ANDROID_HOME`, accept SDK licenses, then run:

```sh
sdkmanager 'platforms;android-35' 'build-tools;35.0.0' 'ndk;27.2.12479018' 'cmake;3.22.1'
./gradlew --no-daemon assembleDebug testDebugUnitTest lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

In the managed Linux cloud environment, `scripts/setup-cloud.sh` installs pinned
tools under `/workspace/tooling`, preserves certificate verification, and builds
and tests the project. Subsequent runs use `scripts/build.sh`. The script supplies
writable Android/Gradle caches and Java proxy/CA configuration. No separate Git
worktree is needed; use this isolated checkout.

GitHub Actions builds the APK and uploads it with reports after pushes to `main`,
pull requests, and manual runs. Open **Actions → Build Android APK → Artifacts**.
For Play Store distribution, use your own protected release signing key and a
release build; the included debug build is for development and sideloading.

## Validation and screenshots

Robolectric runs the Android 15 UI without an emulator. Tests cover empty-state
import/file-picker behavior, saved-library restoration, player progress layout,
service initialization, and archive handling. Native-graphics tests render
the screenshots in `docs/screenshots/`. Screenshots use a small metadata fixture
and paused/live service-state fixtures. Live-spectrum images feed synthetic
multitone PCM through the actual FFT. They are **Robolectric renders, not device
captures**.

The supplied Donkey Kong Country ZIP and Street Fighter II Turbo RSN were tested
separately: **29 + 36 SPC tracks** extracted, metadata read, and decoded to
non-silent 44.1 kHz stereo PCM through the same Java/JNI bridge and GME source used
by the Android app. Every track passed seeking and post-seek rendering. Corrupt
SPC/RSN, archive traversal, and oversized extraction checks also passed.

To reproduce sample tests on Linux with g++ and the SDK/JDK above:

```sh
scripts/test-samples.sh '/path/to/Donkey Kong Country (EMU).zophar.zip' /path/to/sf2t.rsn
```

This tests host-compiled native decoding, not physical-device sound output.
Device audio routing, Bluetooth, lock-screen controls, and background playback
still need a real-device listening pass. Additional console formats are enabled
in GME but have not been checked against user-supplied sample files.

## Implementation and licenses

Java Android views + Canvas pixel spectrum, an AudioTrack foreground service
with MediaSession controls, unmodified Game Music Emu linked as a shared library,
unarr for RSN extraction, and a BIOS-free Audio Overload/PeOPS PSF decoder. See [THIRD_PARTY.md](../THIRD_PARTY.md) for licenses,
source provenance, and rebuilding/replacing the LGPL decoder.

## MIDI and SoundFonts

Import `.mid` / `.midi` through **Import music**, individually or inside a ZIP/RSN.
The built-in **Pixel GM** bank provides an original, synthetic retro sound with
128 General MIDI program slots and percussion. It is about 27 kB, using simple
waveforms rather than recorded acoustic instruments. An original 16-second
[Pixel Steps MIDI demo](../downloads/ChipPlayer-MIDI-demo.mid) is available to try.

Open **SoundFonts…** from the app menu to import uncompressed `.sf2` banks, see
sizes, select a default, rename or delete imported banks. Files are copied once
into private storage. This version accepts banks up to **64 MiB** to keep mobile
memory use manageable; SF3, SFZ, DLS and compressed/archive banks are not supported.
Use an extracted `.sf2` file. The built-in bank remains available as a fallback.

Hold a MIDI track and choose **Choose SoundFont…**, or use **Current track
SoundFont…** in the menu, to override the default for that track. Choose **Follow
default bank** to clear its override. Changing a bank restores the current track's
position and paused/playing state after seeking. Deleting a bank clears its
references; original source files remain untouched. WAV exports use the selected
bank in an independent synthesizer.

Playback supports tempo maps, running status, GM program/bank changes, velocity,
sustain, pan/volume/expression and pitch bend, with 128 synthesis voices. Standard
MIDI type 2, SMPTE timing and Doom MUS containers are not supported. SysEx and
pressure messages are not synthesized. This lightweight synth does not implement
every SF2 modulator/effect or vendor-specific GS/XG extension. Custom game banks
may need matching MIDI instrument mappings. MIDI Loop repeats the complete file
at its end marker; it does not infer game-specific loop markers. Straight playback
allows a two-second release/fade tail.

No Mario/Doom samples or other game-derived SoundFonts are bundled. User-imported
banks remain subject to their own licences.
