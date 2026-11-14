# PlayStation XA / VAG audio

Version 1.23.0 adds an original, disk-backed ADPCM decoder alongside GME, PSF,
USF and MIDI. No BIOS, console ROM or game audio is bundled.

## Supported layouts

| Format | Support |
| --- | --- |
| XA raw 2352-byte sectors | Mode 2, Form 1 / Form 2 audio |
| XA 2336-byte sectors | Mode 2 subheaders retained, 16-byte sync/header stripped |
| RIFF CDXA | Chunked container with sector data, including unknown/odd-sized chunks |
| XA coding | 4/8-bit ADPCM; mono/stereo; 37800/18900 Hz |
| XA multiplexing | File/channel IDs become separate tracks; EOF closes a segment, so later reuse becomes a new track |
| VAG | Big-endian `VAGp`, standard 48-byte header, mono PSX ADPCM |
| VAG looping | Loop-start block flag when Repeat is enabled; otherwise straight playback to end |

Headerless XA payloads, little-endian/proprietary VAG formats and stereo VAG
layouts are not supported. RIFF WAVE/Maxis XA is a different format, despite its
extension. XA emphasis/de-emphasis filtering and the console's hardware-specific
resampling filters are not emulated; playback uses linear interpolation to the
app's 44.1 kHz stereo output. Mono audio is duplicated to both output channels.

## Import and playback

XA/VAG files are admitted by the common importer, including ZIP/RAR sets and
folder selection. Per-file allowance is 128 MiB for these formats; 32 MiB remains
the default for other extracted content. Input/selection size remains 128 MiB,
expanded set size remains 256 MiB and the library still limits sets to 1000 tracks.
XA file/channel/segment labels are prefixed with the original filename; VAG uses
the original filename. Both report Sony PlayStation for library grouping.

The decoder indexes compressed blocks and streams a small PCM buffer, not a full
uncompressed recording. Predictor checkpoints are populated on demand. Forward
seeking builds checkpoints while decoding; backward/repeated seeks use them.
The existing bounded, interruptible seek steps remain on the audio worker.
Each stream and decoder has independent histories, including WAV export.
These formats expose no instrument mute/solo API; the mixer is disabled.

## Verification

`tests/make-psx-audio-fixtures.py` generates original ADPCM and an independent
integer PCM reference. `PsxAudioNativeSmoke` compares every output sample for
11 reference streams, including both XA bit depths/channel layouts, Form 1,
2336-byte/RIFF containers and a three-track multiplexed file with channel reuse.
It checks seeks, duration, loop, isolated export, malformed inputs and ZIP import.
VAG mono and 4-bit stereo XA reference PCM were independently cross-checked
against the installed FFmpeg decoders (before the defined linear resampling).

`PsxAudioCheck.cpp` exercises 1250 deterministic valid/mutated opens, renders,
seeks and loops under AddressSanitizer and UndefinedBehaviorSanitizer:

```sh
g++ -std=c++11 -O1 -g -fsanitize=address,undefined -fno-omit-frame-pointer \
  -Iapp/src/main/cpp app/src/main/cpp/psx_audio.cpp tests/PsxAudioCheck.cpp \
  -o /tmp/chipamp-psx-audio-check
/tmp/chipamp-psx-audio-check app/build/sample-check/psx-audio
```

These checks use synthetic fixtures, not a claim that every game's XA/VAG variant
has been tested. Unsupported layouts fail explicitly rather than guessing a rate
or channel layout.
