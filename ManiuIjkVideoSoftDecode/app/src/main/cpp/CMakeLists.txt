cmake_minimum_required(VERSION 3.10.1)
include_directories(include)
# 系统环境变量一样  多平台  头文件 一个   平台  库  接口  头文件   实现类    so
message(${CMAKE_SOURCE_DIR})
message(${ANDROID_ABI})
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}")

project("maniuijk")
add_library(
        maniuijk
        SHARED
        native-lib.cpp
        MNQueue.cpp)
find_library(
        log-lib
        log)
find_library(
        android-lib
        android)
target_link_libraries(
        maniuijk
        avcodec
        avfilter
        avformat
        avutil
        swresample
        swscale
        log
        android)