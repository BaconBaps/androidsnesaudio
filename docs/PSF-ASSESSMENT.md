# PlayStation PSF assessment

Implemented 24 October 2026 for v1.13.0. Original PlayStation PSF1 playback
is integrated alongside the existing formats, including RSN import. See
[validation evidence](VALIDATION.md) for test scope and remaining device checks.

## Implemented behavior

- Support original PlayStation PSF version 1: `.psf` and `.minipsf` tracks.
- Import ZIP sets containing shared `.psflib` dependencies. Libraries are retained
  privately but do not become playable entries.
- Preserve relative folder structure for PSF dependencies. The existing flat
  archive extraction cannot resolve references reliably.
- Enforce an import-root boundary, bounded extraction, dependency-depth and cycle
  checks; reject missing libraries with a readable error. A standalone minipsf
  selected through the document picker does not grant access to sibling files,
  so dependent tracks should be imported as a ZIP set.
- Route decoded stereo PCM through the existing player, visualizer and WAV export;
  retain favourites, sleep timer, widget and library grouping.
- Parse PSF metadata and length/fade tags, with explicit fallback timing. Do not
  describe PSF2 or other xSF variants as supported without their own decoders.

## Dependencies checked

**psflib** (evaluated, not bundled) provides the container and library-chain loader under the MIT licence:
https://github.com/kode54/psflib/blob/95509e0c6f13d769593bbf51a1b0e0efdc355ba1/LICENSE

It does not emulate the PlayStation CPU or sound hardware by itself.

**Highly Experimental** exposes per-instance PSX emulation, but its readme
explicitly describes its dependency on Sony BIOS code. Its current `bios.c`
accepts a BIOS image from the caller. That is not a ready-to-ship BIOS-free
solution, and its repository does not provide a clear top-level redistribution
licence for the emulator core:
https://github.com/kode54/Highly_Experimental/blob/ccffd7b5f21ab8feaed78544b677e7da0ad43ce7/Core/Readme.txt

**Audacious's PSF plugin** supplies BIOS-service emulation and a PSF engine. Its
README describes mixed BSD/GPL licensing, and its PeOPS sound core includes the
GPL version 2 licence:
https://github.com/audacious-media-player/audacious-plugins/tree/419f26c28cdf430f4af2d9f659befcc3474aeae5/src/psf

The previous app used Junrar under the UnRAR licence, which prohibits using its
source to develop a RAR archiver. This build replaces Junrar with LGPLv3 unarr
and distributes the combined APK under GPLv3. Original app MIT grants remain.
No UnRAR-licensed code or proprietary BIOS is linked into the new APK.

## RSN-preserving replacement verified on the supplied archive

UnRAR/Junrar is not the only way to import RSN. **unarr** is a separate archive
extractor, derived from The Unarchiver work, under LGPLv3. Its stated purpose
includes RAR filters and solid compression:
https://github.com/selmf/unarr/tree/29084afa8d507b34558f2936ec789edcd116e306

Its licence is available here:
https://github.com/selmf/unarr/blob/29084afa8d507b34558f2936ec789edcd116e306/COPYING

Host verification on 24 October 2026:

- Compiled the RAR reader from that pinned unarr revision.
- Extracted the supplied `sf2t.rsn`: **37 entries, including 36 SPC tracks**,
  totaling **2,383,802 bytes**.
- Independently extracted the same archive with the previous app's Junrar 7.5.5.
- Compared each entry's original filename, uncompressed length, and SHA-256:
  **every entry matched**.
- Fixture archive SHA-256:
  `396f0bfdad65164fff99ce4e58ad54d53140812847ccb6765485305f97b3c6d1`.

Libarchive was also checked. The installed 3.7.4 reader fails on this same
archive with `RAR solid archive support unavailable.` The latest source checked
(`8509b99a248c52b676491a8675c3f92c7b5673bc`) still has this rejection in its RAR4
reader. It is therefore not an adequate replacement for the current RSN feature.

## Integrated implementation

- The pinned unarr RAR reader replaces Junrar through JNI. Every entry is drained
  in sequence to retain solid dictionaries, including ignored files. Existing
  import limits and rollback remain in place. Unsupported/encrypted archives fail
  with an error; this does not claim support for every RAR variant.
- A bounded PSF1 loader resolves primary and auxiliary libraries, checks CRCs,
  preserves archive paths within the private import tree, and rejects missing or
  cyclic dependencies. Dependency files do not appear as tracks.
- The BIOS-free Audio Overload / PeOPS core runs on an instance-owned worker.
  Mutable emulator state is thread-local, so playback and WAV export can coexist.
  Backward seeks recreate the worker; forward seeks emulate and discard audio.
  CPU/HLE execution budgets report a driver error rather than an unbounded loop.
- PSF metadata, length/fade tags, looping, platform grouping, the visualizer and
  existing WAV export use the same app controls. PSF2 and MIDI are not implemented.
- The corresponding source and licences are included. See [THIRD_PARTY.md](../THIRD_PARTY.md).

Native tests use an original generated MIPS/SPU tone in standalone and shared-library
PSF containers. They validate audible PCM, seeks, timing, UTF-8 metadata, independent
instances, ZIP import and export. They do not establish compatibility with every
commercial game's driver. No real-game PSF set was supplied; device listening and
real-game compatibility testing remain for the test release.
