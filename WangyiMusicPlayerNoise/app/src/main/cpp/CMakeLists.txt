
cmake_minimum_required(VERSION 3.4.1)

include_directories(include)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}")
message(-------------)
message(${CMAKE_SOURCE_DIR})
aux_source_directory(. SOURCE)

#因为直接是源码依赖, 所以没有平台限制
include_directories(soundtouch/include)
include_directories(soundtouch/SoundTouch)
#添加文件变量 SOURCE1
aux_source_directory(./soundtouch/SoundTouch SOURCE1)

#add_library(
#        native-lib
#        SHARED
#        native-lib.cpp )
add_library(
        native-lib
        SHARED
        ${SOURCE}
        MNFFmpeg.cpp
        MNCallJava.cpp
        MNAudio.cpp
        MNPlaystatus.cpp
         ${SOURCE1}
)
find_library(
        log-lib
        log )
target_link_libraries(
        native-lib
        avcodec
        avdevice
        avfilter
        avformat
        avutil
        swresample
        swscale
        ${log-lib}
        OpenSLES
        android)