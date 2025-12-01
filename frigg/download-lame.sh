#!/bin/bash

set -e

LAME_VERSION="3.100"
LAME_URL="https://sourceforge.net/projects/lame/files/lame/${LAME_VERSION}/lame-${LAME_VERSION}.tar.gz/download"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAME_DIR="${SCRIPT_DIR}/src/native/lame"
TEMP_DIR=$(mktemp -d)

echo "Downloading LAME ${LAME_VERSION}..."

cd "${TEMP_DIR}"
curl -L -o "lame-${LAME_VERSION}.tar.gz" "${LAME_URL}"

echo "Extracting LAME..."
tar -xzf "lame-${LAME_VERSION}.tar.gz"

echo "Copying LAME source to ${LAME_DIR}..."
mkdir -p "${LAME_DIR}"
cp -r "lame-${LAME_VERSION}"/* "${LAME_DIR}/"

echo "Cleaning up..."
rm -rf "${TEMP_DIR}"

echo "LAME ${LAME_VERSION} downloaded successfully to ${LAME_DIR}"



