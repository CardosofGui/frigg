#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAME_DIR="${SCRIPT_DIR}/src/native/lame"
WRAPPER_DIR="${SCRIPT_DIR}/src/native/wrapper"
BUILD_DIR="${SCRIPT_DIR}/build/ios-lame"
LIB_DIR="${SCRIPT_DIR}/src/native/lib"

if [ ! -d "${LAME_DIR}/libmp3lame" ]; then
    echo "LAME source not found. Please run download-lame.sh first."
    exit 1
fi

echo "Generating config.h using configure..."
cd "${LAME_DIR}"

TEMP_BUILD_DIR=$(mktemp -d)
cd "${TEMP_BUILD_DIR}"

"${LAME_DIR}/configure" \
    --disable-shared \
    --enable-static \
    --disable-frontend \
    --prefix="${TEMP_BUILD_DIR}/install" \
    > /dev/null 2>&1 || true

if [ -f "${TEMP_BUILD_DIR}/config.h" ]; then
    cp "${TEMP_BUILD_DIR}/config.h" "${LAME_DIR}/config.h"
    echo "config.h generated successfully"
else
    echo "Warning: Could not generate config.h, using fallback"
fi

rm -rf "${TEMP_BUILD_DIR}"
cd "${SCRIPT_DIR}"

mkdir -p "${LIB_DIR}"

ARCHS=("arm64" "x86_64" "arm64-simulator")
MIN_IOS_VERSION="11.0"

for ARCH in "${ARCHS[@]}"; do
    echo "Building for ${ARCH}..."
    
    if [ "${ARCH}" == "arm64-simulator" ]; then
        SDK="iphonesimulator"
        ARCH_NAME="arm64"
    elif [ "${ARCH}" == "x86_64" ]; then
        SDK="iphonesimulator"
        ARCH_NAME="x86_64"
    else
        SDK="iphoneos"
        ARCH_NAME="arm64"
    fi
    
    BUILD_ARCH_DIR="${BUILD_DIR}/${ARCH}"
    mkdir -p "${BUILD_ARCH_DIR}"
    
    SDK_PATH=$(xcrun --sdk ${SDK} --show-sdk-path)
    
    cd "${BUILD_ARCH_DIR}"
    
    cmake "${SCRIPT_DIR}/src/native" \
        -DCMAKE_SYSTEM_NAME=iOS \
        -DCMAKE_OSX_ARCHITECTURES=${ARCH_NAME} \
        -DCMAKE_OSX_DEPLOYMENT_TARGET=${MIN_IOS_VERSION} \
        -DCMAKE_OSX_SYSROOT=${SDK_PATH} \
        -DCMAKE_BUILD_TYPE=Release \
        -DBUILD_SHARED_LIBS=OFF \
        -DCMAKE_C_COMPILER="$(xcrun --sdk ${SDK} --find clang)" \
        -DCMAKE_CXX_COMPILER="$(xcrun --sdk ${SDK} --find clang++)" \
        -DCMAKE_C_FLAGS="-arch ${ARCH_NAME} -isysroot ${SDK_PATH} -mios-version-min=${MIN_IOS_VERSION} -fembed-bitcode" \
        -DCMAKE_CXX_FLAGS="-arch ${ARCH_NAME} -isysroot ${SDK_PATH} -mios-version-min=${MIN_IOS_VERSION} -fembed-bitcode"
    
    make -j$(sysctl -n hw.ncpu)
    
    cd "${SCRIPT_DIR}"
done

echo "Creating universal libraries..."

DEVICE_LIB="${BUILD_DIR}/arm64/libwav_to_mp3.a"
DEVICE_LAME_LIB="${BUILD_DIR}/arm64/liblame.a"

SIMULATOR_ARM64_LIB="${BUILD_DIR}/arm64-simulator/libwav_to_mp3.a"
SIMULATOR_ARM64_LAME_LIB="${BUILD_DIR}/arm64-simulator/liblame.a"
SIMULATOR_X86_64_LIB="${BUILD_DIR}/x86_64/libwav_to_mp3.a"
SIMULATOR_X86_64_LAME_LIB="${BUILD_DIR}/x86_64/liblame.a"

OUTPUT_DEVICE_LIB="${LIB_DIR}/libfrigg_ios_arm64.a"
OUTPUT_SIMULATOR_LIB="${LIB_DIR}/libfrigg_ios_simulator.a"

if [ ! -f "${DEVICE_LIB}" ] || [ ! -f "${DEVICE_LAME_LIB}" ]; then
    echo "Error: Device libraries not found"
    exit 1
fi

if [ ! -f "${SIMULATOR_ARM64_LIB}" ] || [ ! -f "${SIMULATOR_ARM64_LAME_LIB}" ]; then
    echo "Error: Simulator arm64 libraries not found"
    exit 1
fi

if [ ! -f "${SIMULATOR_X86_64_LIB}" ] || [ ! -f "${SIMULATOR_X86_64_LAME_LIB}" ]; then
    echo "Error: Simulator x86_64 libraries not found"
    exit 1
fi

echo "Creating universal simulator library for wav_to_mp3..."
lipo -create "${SIMULATOR_ARM64_LIB}" "${SIMULATOR_X86_64_LIB}" -output "${LIB_DIR}/libwav_to_mp3_simulator.a"

echo "Creating universal simulator library for lame..."
lipo -create "${SIMULATOR_ARM64_LAME_LIB}" "${SIMULATOR_X86_64_LAME_LIB}" -output "${LIB_DIR}/liblame_simulator.a"

echo "Combining wav_to_mp3 and lame into final libraries..."

cd "${LIB_DIR}"

libtool -static -o "${OUTPUT_SIMULATOR_LIB}" libwav_to_mp3_simulator.a liblame_simulator.a
libtool -static -o "${OUTPUT_DEVICE_LIB}" "${DEVICE_LIB}" "${DEVICE_LAME_LIB}"

rm -f libwav_to_mp3_simulator.a liblame_simulator.a

echo "Final libraries created successfully!"

cd "${SCRIPT_DIR}"

echo "Verifying libraries..."
if [ -f "${OUTPUT_DEVICE_LIB}" ]; then
    echo "Device library created: ${OUTPUT_DEVICE_LIB}"
    lipo -info "${OUTPUT_DEVICE_LIB}"
else
    echo "Error: Device library not created"
    exit 1
fi

if [ -f "${OUTPUT_SIMULATOR_LIB}" ]; then
    echo "Simulator library created: ${OUTPUT_SIMULATOR_LIB}"
    lipo -info "${OUTPUT_SIMULATOR_LIB}"
else
    echo "Error: Simulator library not created"
    exit 1
fi

echo "iOS build complete! Libraries are in ${LIB_DIR}"
