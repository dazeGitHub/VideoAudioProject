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

#引入 liblog.so, 在系统环境目录 : ndk-bundle/toolchains/llvm/prebuilt/windows-x86_64/sysroot/usr/lib/aarch64-linux-android/28/liblog.so
find_library(
        log-lib
        log)

#引入 libandroid.so
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
#使用这种方式也可以 : target_link_libraries(${log-lib}  ${android-lib})
