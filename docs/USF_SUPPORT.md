# Nintendo 64 USF support

Implemented in v1.18.0. `.usf` and `.miniusf` are playable tracks; `.usflib`
files are retained as dependencies and hidden from the playlist.

## Decoder and import

The shared engine recognizes PSF container version 0x21 and routes it to
`UsfDecoder`, separate from PlayStation PSF. LazyUSF2 is pinned to Cog revision
`1ef38735c065da74c499eda05e0ae7b1e953586c`; see `third_party/lazyusf2` and
THIRD_PARTY.md for source and licences. The build uses the portable cached CPU
interpreter and low-level RSP on ARM64 and x86_64. It does not use executable
memory, a proprietary BIOS or a game ROM.

ZIP and folder import preserve relative paths, including shared libraries in
sibling folders; references are case-insensitive and confined to the private
imported set. The existing extraction limits/rollback apply. Select the full
folder or ZIP when a miniUSF references companion files. The platform tag is
Nintendo 64, using the existing collapsible library grouping.

The loader validates header version, compressed data checksum, sparse-section
structure, destination bounds, state header and RAM size. ROM/state patches are
merged in primary-library, track, numbered-library order. Metadata from an
auxiliary library does not overwrite the track's own metadata. UTF-8 and CP1252
tags, length/fade, _enablecompare and _enablefifofull are handled.

Bounds: each file up to 32 MiB; at most 64 dependency loads, depth 16 and 128 MiB
of loaded dependency data; state up to 8 MiB RAM plus its header; ROM patches up
to 64 MiB. Invalid/missing/cyclic/escaping references report import errors.

## Playback

Each decoder owns its state and produces 44.1 kHz stereo PCM. Loop suppresses
the timed fade without restarting the sound program. Without a length tag,
normal playback lasts three minutes plus a five-second fade. Export uses a
separate state, including when playback is active.

Backward seeks restart and re-emulate; forward seeks render/discard PCM through
the same resampler, preserving sample alignment. The existing playback/export
seek scheduler yields between steps, updates the SEEKING indicator, and accepts
pause, replacement seeks and track changes. Long seeks can take time on phones.

Local core changes bound CPU and RSP instruction execution to 64 million steps
and a two-second wall-clock deadline per output chunk (checked every 4096
instructions), bound error strings, check audio DMA against RDRAM and mask
DAC-rate values. A no-audio CPU loop fails rather than blocking indefinitely;
subsequent decoders remain usable. This is an execution guard, not a claim that
the legacy emulator is a security sandbox.

## Verification

`tests/make-usf-fixtures.py` generates an original N64 MIPS/AI sine-tone program
and mini/library, Unicode, CP1252, sparse patch, invalid state, invalid DMA and
no-audio-loop variants. No game rip, ROM or music sample is distributed.
`tests/UsfNativeSmoke.java` verifies metadata/timing, sustained PCM, seeks,
fades, loop, isolated WAV export, concurrent instances, ZIP dependency import,
error handling and decoder recovery. `scripts/test-samples.sh` includes it
alongside SPC/RSN, VGZ, PlayStation PSF and MIDI regression checks.

AddressSanitizer and leak checks also passed for valid render/reset/seek and
invalid sparse sections, states, paths, checksums, DMA and no-audio programs.
Reproduce the standalone sanitizer check after generating fixtures:

```sh
source scripts/cloud-env.sh
cmake_bin="$ANDROID_HOME/cmake/3.22.1/bin/cmake"
"$cmake_bin" -S app/src/main/cpp -B /tmp/chipplayer-usf-asan \
  -DCMAKE_BUILD_TYPE=RelWithDebInfo \
  -DCMAKE_C_FLAGS='-fsanitize=address -fno-omit-frame-pointer'
"$cmake_bin" --build /tmp/chipplayer-usf-asan --target lazyusf2 -j4
g++ -std=c++11 -O1 -g -fsanitize=address -fno-omit-frame-pointer \
  -Iapp/src/main/cpp -Ithird_party/lazyusf2 tests/UsfDecoderCheck.cpp \
  app/src/main/cpp/usf_decoder.cpp /tmp/chipplayer-usf-asan/liblazyusf2.a \
  -lz -lm -o /tmp/usf-check
python3 tests/make-usf-fixtures.py /tmp/usf-fixtures
ASAN_OPTIONS=detect_leaks=1 /tmp/usf-check /tmp/usf-fixtures
```

Android builds, Robolectric tests and lint pass. Real game-driver compatibility,
CPU/battery use and seek speed still need testing on an Android device; no
emulator or real N64 game set was used in this environment.
