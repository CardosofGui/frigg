cmake_minimum_required(VERSION 3.18.1)
project(frigg)

set(LAME_VERSION "3.100")
set(LAME_DIR "${CMAKE_CURRENT_SOURCE_DIR}/lame")

if(NOT EXISTS "${LAME_DIR}/include/lame.h")
    message(FATAL_ERROR "LAME source code not found. Please run the build script to download LAME.")
endif()

set(LAME_SOURCES
    ${LAME_DIR}/libmp3lame/VbrTag.c
    ${LAME_DIR}/libmp3lame/bitstream.c
    ${LAME_DIR}/libmp3lame/encoder.c
    ${LAME_DIR}/libmp3lame/fft.c
    ${LAME_DIR}/libmp3lame/gain_analysis.c
    ${LAME_DIR}/libmp3lame/id3tag.c
    ${LAME_DIR}/libmp3lame/lame.c
    ${LAME_DIR}/libmp3lame/newmdct.c
    ${LAME_DIR}/libmp3lame/presets.c
    ${LAME_DIR}/libmp3lame/psymodel.c
    ${LAME_DIR}/libmp3lame/quantize.c
    ${LAME_DIR}/libmp3lame/quantize_pvt.c
    ${LAME_DIR}/libmp3lame/reservoir.c
    ${LAME_DIR}/libmp3lame/set_get.c
    ${LAME_DIR}/libmp3lame/tables.c
    ${LAME_DIR}/libmp3lame/takehiro.c
    ${LAME_DIR}/libmp3lame/util.c
    ${LAME_DIR}/libmp3lame/vbrquantize.c
    ${LAME_DIR}/libmp3lame/version.c
    ${LAME_DIR}/libmp3lame/mpglib_interface.c
    ${LAME_DIR}/mpglib/common.c
    ${LAME_DIR}/mpglib/dct64_i386.c
    ${LAME_DIR}/mpglib/decode_i386.c
    ${LAME_DIR}/mpglib/interface.c
    ${LAME_DIR}/mpglib/layer1.c
    ${LAME_DIR}/mpglib/layer2.c
    ${LAME_DIR}/mpglib/layer3.c
    ${LAME_DIR}/mpglib/tabinit.c
)

file(GLOB VECTOR_SOURCES "${LAME_DIR}/libmp3lame/vector/*.c")
if(VECTOR_SOURCES)
    list(APPEND LAME_SOURCES ${VECTOR_SOURCES})
endif()

add_library(lame STATIC ${LAME_SOURCES})

target_include_directories(lame PUBLIC
    ${LAME_DIR}/libmp3lame
    ${LAME_DIR}/include
    ${LAME_DIR}/mpglib
    ${CMAKE_CURRENT_SOURCE_DIR}/lame
)

target_compile_definitions(lame PRIVATE
    HAVE_MPGLIB
    HAVE_CONFIG_H
    WORDS_BIGENDIAN=0
)

if(ANDROID)
    target_compile_definitions(lame PRIVATE
        HAVE_CONFIG_H
    )
endif()

if(ANDROID)
    find_library(log-lib log)
    
    set(JNI_SOURCE "${CMAKE_CURRENT_SOURCE_DIR}/../androidMain/cpp/lame_jni.cpp")
    
    if(NOT EXISTS "${JNI_SOURCE}")
        message(FATAL_ERROR "JNI source file not found: ${JNI_SOURCE}")
    endif()
    
    add_library(wav_to_mp3 SHARED
        wrapper/wav_to_mp3.c
        ${JNI_SOURCE}
    )
    
    target_link_libraries(wav_to_mp3
        lame
        ${log-lib}
    )
    
    target_include_directories(wav_to_mp3 PRIVATE
        ${LAME_DIR}/libmp3lame
        ${LAME_DIR}/include
        ${LAME_DIR}/mpglib
        ${CMAKE_CURRENT_SOURCE_DIR}/wrapper
        ${CMAKE_CURRENT_SOURCE_DIR}/../androidMain/cpp
    )
    
    find_library(jnigraphics-lib jnigraphics)
    target_link_libraries(wav_to_mp3 ${jnigraphics-lib})
else()
    add_library(wav_to_mp3 STATIC
        wrapper/wav_to_mp3.c
    )
    
    target_link_libraries(wav_to_mp3
        lame
    )
    
    target_include_directories(wav_to_mp3 PRIVATE
        ${LAME_DIR}/libmp3lame
        ${LAME_DIR}/include
        ${CMAKE_CURRENT_SOURCE_DIR}/wrapper
    )
endif()

