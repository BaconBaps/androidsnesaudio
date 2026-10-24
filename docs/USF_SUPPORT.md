# Nintendo 64 USF feasibility

Investigated 24 October 2026. USF is not implemented in v1.17.3.

USF uses the PSF container with version byte 0x21, but contains Nintendo 64
state and sparse ROM/state patches, so the PlayStation PSF decoder cannot play
it. A separate N64 audio emulator is required.

## Candidate

LazyUSF2 is present in [Cog](https://github.com/losnoco/Cog/tree/1ef38735c065da74c499eda05e0ae7b1e953586c/Frameworks/lazyusf2/lazyusf2).
Its Mupen64Plus CPU sources carry GPL-2.0-or-later headers and its low-level RSP
core uses CC0. These are promising for the existing GPLv3 APK; audit the exact
compiled file set and retain notices before vendoring. The older LazyUSF core
contains Project64 noncommercial restrictions and should not be substituted.

The public API exposes per-decoder state, sparse section loading and resampled
stereo output. Its documented portable path is a cached interpreter; do not
assume the x86 recompiler will work on Android ARM64.

## Implementation and verification

- Recognize .usf and .miniusf as tracks; keep .usflib dependencies in their
  original relative directory tree when importing ZIPs or folders. Reuse the
  bounded dependency resolution approach from PSF, including cycle detection
  and case-insensitive lookup within the private imported set.
- Load version 0x21 reserved sections in dependency order, decode metadata and
  duration/fade tags, and honour _enablecompare and _enablefifofull settings.
- Validate sparse section lengths, offsets and aggregate allocations before
  calling the core. The candidate upload routine alone does not bounds-check
  every patch destination.
- Add a decoder to the shared engine at 44.1 kHz, with bounded execution for
  malformed/no-audio programs, independent states for playback and WAV export,
  graceful failure, and pause/exit/track-switch recovery.
- Seeking must replay emulation; implement cancellation and an indication while
  seeking, and measure both startup and scrubbing on an ARM64 phone.
- Add original synthetic audio fixtures plus tests for mini/library imports,
  metadata, output, loops, fades, concurrent export, malformed files and
  missing/cyclic/escaping dependencies. Check real user-provided sets as well.
- Group tracks as Nintendo 64 in the existing platform library. Keep SPC/RSN,
  PSF, MIDI and the other native decoder smoke tests passing.

No N64 decoder or game audio is bundled as part of the scrolling fix.
