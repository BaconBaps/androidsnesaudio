# projectM 4.1.8

Unmodified build sources from https://github.com/projectM-visualizer/projectm
at 4967dc003e5206d7df3ff447287f7609c47d2343 (v4.1.8).
Vendored projectm-eval is e8c311e86f4d8c082e96e02b6a33525034eee6d4 (the upstream pinned submodule); see its own license.
Documentation artwork, CI, and upstream tests are omitted. No upstream presets
are bundled in the APK. ChipAmp9x starter presets are original MIT-licensed work.

Built for OpenGL ES 3, with the portable projectm-eval interpreter (no JIT),
without SDL, playlist library, system dependencies or installation targets.
Our JNI bridge, lifecycle, audio capture and preset importer live in app/src.
The parent CMake supplies version components because upstream 4.1.x expects
the top-level project version. Sources retain LGPL-2.1-or-later and dependency notices. GLM and hlslparser use
MIT; projectm-eval uses MIT; SOIL2 includes public-domain, MIT/MIT-0 and Apache-2.0 code.
COPYING-ChipAmp9x.txt adds the GLM MIT text omitted from the upstream snapshot.
