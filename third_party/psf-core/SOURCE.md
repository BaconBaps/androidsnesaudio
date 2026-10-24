# PSF core provenance

Selected files from Audacious plugins revision
`419f26c28cdf430f4af2d9f659befcc3474aeae5`, `src/psf/`:
https://github.com/audacious-media-player/audacious-plugins

Audio Overload CPU/hardware glue has the BSD notices in its source. PeOPS files
are GPL version 2 or, at the user's option, any later version. This distribution
chooses GPL version 3 for the combined binary; see the repository COPYING-GPL-3.
Original authors and notices are retained.

Local changes:

- Replaced Audacious-specific `ao.h` with a standalone little-endian shim.
- Mutable static variables, and exported emulated RAM/refresh state, use C++
  `thread_local`. Each PSF decoder owns a worker thread, including its own CPU,
  RAM, SPU, ADSR and reverb history. Restart/backwards seek recreates that worker
  so TLS starts clean. Read-only constants remain shared.
- Bound invalid ADPCM predictor indexes to avoid an out-of-bounds lookup.
- Made the BIOS PRNG instance-local and bounded CPU/HLE execution loops to
  report a driver error instead of leaving the audio worker stuck.
- Normalized source line endings.

PSF loading, bounded dependencies, threading, timing, seeking and JNI integration
are in `app/src/main/cpp/psf_decoder.cpp`. Only the PSF version-1 execution path is
supported. PSF2 references in the shared hardware glue are stubbed, not advertised
as a working PSF2 engine. No proprietary BIOS is supplied or required.
