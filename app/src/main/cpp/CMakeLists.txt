cmake_minimum_required(VERSION 3.22)
project(xelo LANGUAGES CXX)

set(CMAKE_BUILD_TYPE RelWithDebInfo CACHE STRING "Build type" FORCE)
set(BUILD_SHARED_LIBS ON CACHE BOOL "Build shared libraries" FORCE)

if(ANDROID)
    set(CMAKE_POSITION_INDEPENDENT_CODE ON)
endif()

add_subdirectory(unpaircore)
add_subdirectory(Preloader)
add_subdirectory(xelo_init)
add_subdirectory(XeloOverlay)
add_subdirectory(libHttpClient/Build/libHttpClient.Android)