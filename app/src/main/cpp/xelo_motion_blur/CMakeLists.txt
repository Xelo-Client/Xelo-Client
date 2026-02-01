cmake_minimum_required(VERSION 3.18)
project(MotionBlur LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optimization flags
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2 -fvisibility=hidden -ffunction-sections -fdata-sections -fexceptions -w")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O2 -fvisibility=hidden -ffunction-sections -fdata-sections -w")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--gc-sections,--strip-all -s")

include(FetchContent)

FetchContent_Declare(
    fmt
    GIT_REPOSITORY https://github.com/fmtlib/fmt.git
    GIT_TAG 10.2.1
)
FetchContent_MakeAvailable(fmt)

FetchContent_Declare(
    preloader_android
    GIT_REPOSITORY https://github.com/LiteLDev/preloader-android.git
    GIT_TAG main
)
FetchContent_GetProperties(preloader_android)
if(NOT preloader_android_POPULATED)
    FetchContent_Populate(preloader_android)
    
    file(READ "${preloader_android_SOURCE_DIR}/src/pl/Logger.h" LOGGER_CONTENT)
    string(REPLACE "#include <format>" "#include <fmt/format.h>" LOGGER_CONTENT "${LOGGER_CONTENT}")
    string(REPLACE "std::vformat" "fmt::vformat" LOGGER_CONTENT "${LOGGER_CONTENT}")
    string(REPLACE "std::make_format_args" "fmt::make_format_args" LOGGER_CONTENT "${LOGGER_CONTENT}")
    file(WRITE "${preloader_android_SOURCE_DIR}/src/pl/Logger.h" "${LOGGER_CONTENT}")
    
    add_subdirectory(${preloader_android_SOURCE_DIR} ${preloader_android_BINARY_DIR})
endif()

target_link_libraries(preloader PUBLIC fmt::fmt)

include_directories(
    ${CMAKE_SOURCE_DIR}/src
    ${CMAKE_SOURCE_DIR}/src/ImGui
    ${CMAKE_SOURCE_DIR}/src/ImGui/backends
)

set(SOURCES
    src/main.cpp
    src/ImGui/imgui.cpp
    src/ImGui/imgui_draw.cpp
    src/ImGui/imgui_tables.cpp
    src/ImGui/imgui_widgets.cpp
    src/ImGui/backends/imgui_impl_opengl3.cpp
    src/ImGui/backends/imgui_impl_android.cpp
)

add_library(MotionBlur SHARED ${SOURCES})
target_link_libraries(MotionBlur preloader log android EGL GLESv3 GLESv2)
