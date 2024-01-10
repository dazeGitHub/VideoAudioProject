cmake_minimum_required(VERSION 3.18.1)

# 1. 添加 main/cpp/include 下的所有子目录的头文件
include_directories(include)

# 2. 配置环境变量 CMakeLists.txt绝对路径/libs/armeabi-v7a
# 系统环境变量一样  多平台的头文件只有一个, 但是库和平台有关系, 头文件类似于接口, 实现类类似于 so
message(${CMAKE_SOURCE_DIR}) #CMakeLists.txt 的绝对路径
message(${ANDROID_ABI}) #armeabi-v7a
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}")

project("maniuijk")
add_library(
        maniuijk
        SHARED
        native-lib.cpp)
find_library(
        log-lib
        log)

# 3. 添加 so 库
# maniuijk 是主 so 库, 发现下面的库是从前往后依次发现的
# 添加 libs/armeabi-v7a/libavcodec.so 库那么就填 avcodec
# avcodec : 用来编解码
# avfilter : 滤镜, 例如 灰色滤镜、分屏滤镜
# avformat : 万能播放器用到的格式
# avutil : 辅助工具类, 例如获取时间戳
# swresample : 重采样, 合并视频 剪辑视频 的时候要重采样
# swscale : 视频的宽高和显示控件的宽高不同, 将视频显示到对应大小的控件就需要用到该库
target_link_libraries(
        maniuijk
        avcodec
        avfilter
        avformat
        avutil
        swresample
        swscale
        ${log-lib})