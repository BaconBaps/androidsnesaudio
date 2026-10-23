#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [ "$#" -lt 2 ] || [ "$#" -gt 3 ]; then echo 'Usage: scripts/test-samples.sh /path/to/set.zip /path/to/set.rsn [/path/to/track.vgz]' >&2; exit 2; fi
source scripts/cloud-env.sh
scripts/build.sh copyNativeTestDependencies
TEST_DIR="$PWD/app/build/sample-check"
CMAKE_BIN="${ANDROID_HOME}/cmake/3.22.1/bin/cmake"
"$CMAKE_BIN" -S third_party/game-music-emu -B "$TEST_DIR/native" -DGME_BUILD_TESTING=OFF -DGME_BUILD_EXAMPLES=OFF -DGME_BUILD_STATIC=OFF -DCMAKE_BUILD_TYPE=Release
"$CMAKE_BIN" --build "$TEST_DIR/native" -j2
g++ -std=c++11 -shared -fPIC -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" -Ithird_party/game-music-emu app/src/main/cpp/player.cpp -L"$TEST_DIR/native/gme" -lgme '-Wl,-rpath,$ORIGIN' -o "$TEST_DIR/native/gme/libplayer.so"
mkdir -p "$TEST_DIR/classes"
javac -cp "$TEST_DIR/deps/*" -d "$TEST_DIR/classes" app/src/main/java/dev/pixelspc/{Engine,ArchiveImporter,PlaybackSeek,WavExporter}.java tests/{ArchiveSmoke,NativeSmoke,NativeSeekSmoke,NativeExportSmoke}.java
java -cp "$TEST_DIR/classes:$TEST_DIR/deps/*" dev.pixelspc.ArchiveSmoke "$1" "$2"
java -Djava.library.path="$TEST_DIR/native/gme" -cp "$TEST_DIR/classes:$TEST_DIR/deps/*" dev.pixelspc.NativeSmoke "$1" "$2"
java -Djava.library.path="$TEST_DIR/native/gme" -cp "$TEST_DIR/classes:$TEST_DIR/deps/*" dev.pixelspc.NativeSeekSmoke "${@:3}"
java -Djava.library.path="$TEST_DIR/native/gme" -cp "$TEST_DIR/classes:$TEST_DIR/deps/*" dev.pixelspc.NativeExportSmoke "$1" "${@:3}"
