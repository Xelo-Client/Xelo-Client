cmake_minimum_required(VERSION 3.18)
project(MotionBlur LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optimization flags
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2 -fvisibility=hidden -ffunction-sections -fdata-sections -fexceptions -w")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O2 -fvisibility=hidden -ffunction-sections -fdata-sections -w")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--gc-sections,--strip-all -s")

set(SOURCES
    src/main.cpp
)

add_library(MotionBlur SHARED ${SOURCES})

target_include_directories(MotionBlur PRIVATE
    src
    ${CMAKE_CURRENT_SOURCE_DIR}/../Preloader/src
)

find_library(log-lib log)
find_library(android-lib android)
find_library(egl-lib EGL)
find_library(glesv3-lib GLESv3)
find_library(glesv2-lib GLESv2)

target_link_libraries(MotionBlur PRIVATE
    preloader
    ${log-lib}
    ${android-lib}
    ${egl-lib}
    ${glesv3-lib}
    ${glesv2-lib}
)