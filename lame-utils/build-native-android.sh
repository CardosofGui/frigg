#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NATIVE_DIR="${SCRIPT_DIR}/src/native"
BUILD_DIR="${SCRIPT_DIR}/build/native-android"

if [ ! -d "${NATIVE_DIR}/lame/libmp3lame" ]; then
    echo "LAME source not found. Please run download-lame.sh first."
    exit 1
fi

ABIS=("arm64-v8a" "armeabi-v7a" "x86" "x86_64")
NDK_VERSION="26.1.10909125"

if [ -z "${ANDROID_NDK_HOME}" ]; then
    if [ -n "${ANDROID_HOME}" ]; then
        ANDROID_NDK_HOME="${ANDROID_HOME}/ndk/${NDK_VERSION}"
    else
        echo "ANDROID_NDK_HOME or ANDROID_HOME not set"
        exit 1
    fi
fi

if [ ! -d "${ANDROID_NDK_HOME}" ]; then
    echo "NDK not found at ${ANDROID_NDK_HOME}"
    exit 1
fi

TOOLCHAIN_FILE="${ANDROID_NDK_HOME}/build/cmake/android.toolchain.cmake"

if [ ! -f "${TOOLCHAIN_FILE}" ]; then
    echo "Android toolchain file not found at ${TOOLCHAIN_FILE}"
    exit 1
fi

CMAKE=$(which cmake)

if [ -z "${CMAKE}" ]; then
    echo "CMake not found. Please install CMake (brew install cmake)"
    exit 1
fi

echo "Using CMake: ${CMAKE}"
echo "Using NDK toolchain: ${TOOLCHAIN_FILE}"

for ABI in "${ABIS[@]}"; do
    echo "Building for ${ABI}..."
    
    BUILD_ABI_DIR="${BUILD_DIR}/${ABI}"
    mkdir -p "${BUILD_ABI_DIR}"
    
    cd "${BUILD_ABI_DIR}"
    
    case "${ABI}" in
        arm64-v8a)
            ARCH="arm64"
            TOOLCHAIN="aarch64-linux-android"
            ;;
        armeabi-v7a)
            ARCH="arm"
            TOOLCHAIN="arm-linux-androideabi"
            ;;
        x86)
            ARCH="x86"
            TOOLCHAIN="i686-linux-android"
            ;;
        x86_64)
            ARCH="x86_64"
            TOOLCHAIN="x86_64-linux-android"
            ;;
    esac
    
    "${CMAKE}" "${NATIVE_DIR}" \
        -DCMAKE_TOOLCHAIN_FILE="${TOOLCHAIN_FILE}" \
        -DANDROID_ABI="${ABI}" \
        -DANDROID_PLATFORM=android-24 \
        -DCMAKE_BUILD_TYPE=Release \
        -DANDROID_ARM_NEON=ON \
        -DANDROID_STL=c++_shared
    
    make -j$(sysctl -n hw.ncpu 2>/dev/null || nproc)
    
    cd "${SCRIPT_DIR}"
done

echo "Native libraries built successfully!"

