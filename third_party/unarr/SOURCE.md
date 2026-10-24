# unarr provenance

Source: https://github.com/selmf/unarr
Revision: `29084afa8d507b34558f2936ec789edcd116e306`
Version: 1.2.0 source snapshot.

The original common/RAR/LZMA sources, public header template, AUTHORS, README and
LGPLv3 COPYING are retained. There are no local changes to these sources. The app
CMake build selects the RAR reader and PPMd implementation and generates the
public header. It builds `libunarr.so` separately. ZIP extraction remains Java.

This implementation originates from The Unarchiver/unarr; it does not use the
UnRAR-licensed Junrar dependency it replaces. Existing solid RAR4 RSN sets remain
supported. Encrypted archives and RAR5 are not supported by this build.
