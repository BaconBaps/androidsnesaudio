#!/usr/bin/env bash
# Real GLES renderer smoke test, using system EGL/Mesa; no Android emulator required.
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/cloud-env.sh
projectm_cmake="$(command -v cmake || true)"
if [ -z "$projectm_cmake" ]; then projectm_cmake="$ANDROID_HOME/cmake/3.22.1/bin/cmake"; fi
projectm_build="$PWD/app/build/projectm-host"
projectm_headers=/usr/include
if [ ! -f "$projectm_headers/GLES3/gl3.h" ]; then
    projectm_headers="$projectm_build/gl-headers"
    mkdir -p "$projectm_headers"
    for part in GLES2 GLES3 EGL KHR; do
        cp -r "$ANDROID_HOME/ndk/27.2.12479018/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include/$part" "$projectm_headers/"
    done
fi
projectm_gl="$(c++ -print-file-name=libGLESv2.so.2)"
if [ ! -f "$projectm_gl" ]; then projectm_gl="$(c++ -print-file-name=libOpenGL.so.0)"; fi
projectm_egl="$(c++ -print-file-name=libEGL.so.1)"
"$projectm_cmake" -S third_party/projectm -B "$projectm_build" \
    -DCMAKE_BUILD_TYPE=Release -DENABLE_GLES=ON -DENABLE_INSTALL=OFF \
    -DENABLE_SYSTEM_PROJECTM_EVAL=OFF -DENABLE_PLAYLIST=OFF \
    -DENABLE_DEBUG_POSTFIX=OFF -DENABLE_FAST_MATH=OFF \
    -DCMAKE_C_FLAGS=-DSOIL_GLES2 \
    -DOPENGL_gles3_LIBRARY="$projectm_gl" -DOPENGL_GLES3_INCLUDE_DIR="$projectm_headers"
"$projectm_cmake" --build "$projectm_build" -j 4
c++ -std=c++17 -D__GBM__ -O2 tests/ProjectMRenderTest.cpp \
    -I"$projectm_headers" -Ithird_party/projectm/src/api/include \
    -I"$projectm_build/src/api/include" -L"$projectm_build/src/libprojectM" \
    -Wl,-rpath,"$projectm_build/src/libprojectM" -lprojectM-4 "$projectm_egl" "$projectm_gl" \
    -pthread -o "$projectm_build/render-test"
mkdir -p "$projectm_build/cache"
LIBGL_ALWAYS_SOFTWARE=1 MESA_SHADER_CACHE_DIR="$projectm_build/cache" \
    "$projectm_build/render-test" "${PROJECTM_TEXTURE_PATH:-app/src/main/assets/projectm}" \
    app/src/main/assets/projectm/*.milk "$@"
