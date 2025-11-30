#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAME_DIR="${SCRIPT_DIR}/src/native/lame"
WRAPPER_DIR="${SCRIPT_DIR}/src/native/wrapper"
BUILD_DIR="${SCRIPT_DIR}/build/ios-lame"

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

echo "iOS build complete!"
