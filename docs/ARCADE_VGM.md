# Arcade and extended VGM playback

The native dispatcher keeps GME for its existing VGM chip set (SN76489/YM2413/
YM2612). VGM files declaring other chips use pinned libvgm, configured with the
broad device collection. The app exposes VGM/VGZ only; this does not enable QSF,
S98, DRO or PSF2 import. OPL4's GPL-2.0-only core is excluded from this GPLv3 APK.
See `third_party/libvgm/SOURCE.md` for the revision/build selections.

Each decoder owns its own loader, emulator instances and sample state. GD3 text
is decoded with a bounded UTF-16LE-to-UTF-8 adapter so Android API 26 does not need
system iconv. Gzip expansion, data-block lengths and sample-ROM allocations are
bounded. Unknown commands and unsupported chips report an error.

Seeking emulates/discards audio, rather than skipping register writes, preserving
sample playback and oscillator state. Existing Java seek stepping bounds each
worker call to 250 ms. Loop points remain native to the VGM stream; ordinary
playback uses the tagged intro plus two loop passes and an 8-second fade. Turning
loop off after that duration starts the fade from the current position. Each
export uses a separate decoder. The GME mixer is preserved on the existing route;
libvgm channel mixing is intentionally not presented as a fake GME mixer.

## Validation

Private user-supplied archives, not bundled or committed:

- Street Fighter II: Champion Edition (CPS-1): 47/47 tracks opened and generated
  audio within the first 2 seconds.
- Super Street Fighter II (CPS-2): 72/72 tracks likewise.
- The Simpsons (Arcade): 45/45 tracks likewise.

Representative music tracks from all three pass forward/backward seeks, native
loop-boundary crossings, loop toggles and fade/end checks. WAV export from CPS-2
is non-silent at zero and nonzero offsets and does not advance live playback.
Generated validation fixtures cover Unicode GD3, unknown commands, truncated
sample blocks, OPL4 rejection and opening a valid file after a failed import.

The original GME seek/mixer/NSFE, SPC/RSN, PSF, USF, MIDI and XA/VAG checks are
rerun against the combined library. Tests use host JNI plus Android builds and
Robolectric; physical-device playback/performance still needs user testing.

Host tests: `tests/ArcadeVgmSmoke.java`, `ArcadeVgmSetScan.java`, and
`VgmValidationSmoke.java`. Supply paths to your own VGM/VGZ files; the tests do
not download or redistribute game music.
