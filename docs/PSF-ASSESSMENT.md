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

## Concrete options

1. **Continue PSF first with a GPL-compatible build.** Evaluate and replace the
   UnRAR-licensed importer with a compatible RAR extraction implementation,
   preserve RSN support with regression tests, integrate a BIOS-free PSF engine,
   and publish all required source and licence notices for the resulting build.
   This changes dependency and distribution terms; the user's own MIT-licensed
   source need not lose its existing MIT grant.
2. **Keep the current dependency/licensing arrangement while adding MIDI first.**
   Evaluate a permissively licensed MIDI synthesizer and user-selectable
   SoundFonts, and continue investigating a suitable PSF core independently.

No PSF core, BIOS image, new redistribution terms or advertised PSF extension has
been added to the app during this assessment. A decoder decision should precede
implementation. Whichever route is selected needs real sample-set testing and
separate decoder states so import/export cannot corrupt active playback.
