#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [ "$#" -eq 0 ]; then echo 'Usage: scripts/test-arcade.sh /path/to/music.vgz [...]' >&2; exit 2; fi
source scripts/cloud-env.sh
arcade_check_dir="$PWD/app/build/arcade-check"
arcade_native_dir="$PWD/app/build/arcade-native"
arcade_cmake="$ANDROID_HOME/cmake/3.22.1/bin/cmake"
"$arcade_cmake" -S app/src/main/cpp -B "$arcade_native_dir" -DCMAKE_BUILD_TYPE=Release
"$arcade_cmake" --build "$arcade_native_dir" -j2
mkdir -p "$arcade_check_dir"
javac -d "$arcade_check_dir" app/src/main/java/dev/pixelspc/Engine.java tests/{ArcadeVgmSmoke,ArcadeVgmSetScan,VgmValidationSmoke}.java
arcade_native_path="$arcade_native_dir:$arcade_native_dir/gme/gme"
java -Djava.library.path="$arcade_native_path" -cp "$arcade_check_dir" dev.pixelspc.VgmValidationSmoke
java -Djava.library.path="$arcade_native_path" -cp "$arcade_check_dir" dev.pixelspc.ArcadeVgmSmoke "$@"
