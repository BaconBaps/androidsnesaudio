# libvgm source

Upstream: https://github.com/ValleyBell/libvgm
Revision: c5c03a2c2514d38d2ab7b7e8dbd021af0b8292cb

Sources are retained unmodified. Audio backends, example programs, IDE files and
unneeded external-library bundles are omitted; CMake modules are retained.
The app CMake project selects the broad emulation-device collection, statically
links the player/emulation/utils libraries, and disables system audio output,
thread helpers and system iconv. The app supplies a portable bounded UTF-16LE to
UTF-8 adapter for GD3 tags, allowing Android API 26 without an iconv dependency.

Individual source headers retain their authorship and licensing notices
(GPL-2-or-later, LGPL-2.1-or-later, BSD-3-Clause, MIT and original core notices).
The GPL-2.0-only `ymf278b.c` implementation is retained as upstream source but
**not compiled/linked**: OPL4 playback is explicitly rejected by this build.
This avoids combining that core with this app's GPLv3 components. The existing
GPL/LGPL licence texts are shipped in the app; see its licenses directory.

No copyrighted game tracks, sample-ROM sets, or test archives are bundled here.
Runtime VGM samples come exclusively from the user-imported music files.
