cmake_minimum_required(VERSION 3.18.1)
project("x264rtmpmaniu")
message("--------david------------")
#CMAKE_SOURCE_DIR系统变量
message(${CMAKE_SOURCE_DIR})
message(${ANDROID_ABI})
include_directories(${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/include)
message("--------david")
message(${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/include)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/lib")
include_directories(${CMAKE_SOURCE_DIR}/librtmp)
add_subdirectory(librtmp)
#库文件 的

#系统环境目录

file(GLOB cpp_source  *.cpp)
add_library(
        x264rtmpmaniu

        SHARED
        JavaCallHelper.cpp
        ${cpp_source})

target_link_libraries(
        x264rtmpmaniu
        x264
        rtmp
        log)