# PlayStation PSF assessment

Reviewed 24 October 2026. This is an implementation proposal, not a claim of
playback support. The released app still supports its existing formats.

## Intended behavior

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

**psflib** provides the container and library-chain loader under the MIT licence:
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

The current app uses Junrar under the UnRAR licence, which prohibits using its
source to develop a RAR archiver. Adding GPL components to the same APK therefore
requires resolving the distribution-licence compatibility rather than simply
copying the PSF core into the current build.

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
- Independently extracted the same archive with the app's current Junrar 7.5.5.
- Compared each entry's original filename, uncompressed length, and SHA-256:
  **every entry matched**.
- Fixture archive SHA-256:
  `396f0bfdad65164fff99ce4e58ad54d53140812847ccb6765485305f97b3c6d1`.

Libarchive was also checked. The installed 3.7.4 reader fails on this same
archive with `RAR solid archive support unavailable.` The latest source checked
(`8509b99a248c52b676491a8675c3f92c7b5673bc`) still has this rejection in its RAR4
reader. It is therefore not an adequate replacement for the current RSN feature.

## Recommended implementation path

Preserving RSN import is a requirement, rather than a tradeoff for PSF:

1. Integrate the pinned unarr RAR reader on Android and replace Junrar only after
   the existing RSN tests pass. Retain ZIP import and the current extraction
   limits, traversal protections and transactional import behavior.
2. Test solid RAR4, stored and compressed entries, RAR filters, malformed archives,
   duplicate filenames and failure cleanup on both supported Android ABIs.
   The host comparison proves this supplied archive, not every RSN variant.
3. Integrate a BIOS-free PSF decoder and preserve PSF library dependencies inside
   the import boundary. The PeOPS source checked permits GPLv2 **or later**;
   unarr's LGPLv3 can be combined through a GPLv3-compatible distribution path,
   with the required source and licence notices. Preserve existing MIT grants on
   the app's own source; do not silently describe the combined build as MIT-only.
4. Verify PSF playback, seeking, looping, metadata, WAV export and isolated decoder
   states. Re-run SPC/RSN playback and export regression checks before releasing.
5. Add MIDI/SoundFont support after the PSF work.

The host experiment has not replaced the Android importer or added PSF playback.
No BIOS image or new decoder has been bundled, and no APK update is being claimed
by this assessment. The released app retains its existing RSN implementation.
