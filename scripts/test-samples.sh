#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [ "$#" -lt 2 ] || [ "$#" -gt 3 ]; then echo 'Usage: scripts/test-samples.sh /path/to/set.zip /path/to/set.rsn [/path/to/track.vgz]' >&2; exit 2; fi
source scripts/cloud-env.sh
scripts/build.sh
TEST_DIR="$PWD/app/build/sample-check"
CMAKE_BIN="${ANDROID_HOME}/cmake/3.22.1/bin/cmake"
"$CMAKE_BIN" -S app/src/main/cpp -B "$TEST_DIR/native-player" -DCMAKE_BUILD_TYPE=Release
"$CMAKE_BIN" --build "$TEST_DIR/native-player" -j2
mkdir -p "$TEST_DIR/classes"
javac -d "$TEST_DIR/classes" app/src/main/java/dev/pixelspc/{Engine,RarReader,ArchiveImporter,PlaybackSeek,WavExporter}.java tests/{ArchiveSmoke,NativeSmoke,NativeSeekSmoke,NativeExportSmoke,PsfNativeSmoke,MidiNativeSmoke}.java
NATIVE_PATH="$TEST_DIR/native-player:$TEST_DIR/native-player/gme/gme"
java -Djava.library.path="$NATIVE_PATH" -cp "$TEST_DIR/classes" dev.pixelspc.ArchiveSmoke "$1" "$2"
java -Djava.library.path="$NATIVE_PATH" -cp "$TEST_DIR/classes" dev.pixelspc.NativeSmoke "$1" "$2"
java -Djava.library.path="$NATIVE_PATH" -cp "$TEST_DIR/classes" dev.pixelspc.NativeSeekSmoke "${@:3}"
java -Djava.library.path="$NATIVE_PATH" -cp "$TEST_DIR/classes" dev.pixelspc.NativeExportSmoke "$1" "${@:3}"
python3 tests/make-psf-fixtures.py "$TEST_DIR/psf"
java -Djava.library.path="$NATIVE_PATH" -cp "$TEST_DIR/classes" dev.pixelspc.PsfNativeSmoke "$TEST_DIR/psf"

python3 tests/make-midi-fixtures.py "$TEST_DIR/midi"
java -Djava.library.path="$NATIVE_PATH" -cp "$TEST_DIR/classes" dev.pixelspc.MidiNativeSmoke "$TEST_DIR/midi" app/src/main/assets/soundfonts/Pixel-GM.sf2 downloads/ChipPlayer-MIDI-demo.mid
