cmake_minimum_required(VERSION 3.18.1)
project("x264rtmpmaniu")
#message() 打印的信息指只会 app/.cxx/Debug/xxx/x86或armeabi-v7a/CMakeFiles/CMakeOutput.log 里面, 而不是日志里面
message("--------david------------")

#CMAKE_SOURCE_DIR 是系统变量, 在如下文件定义 :
#app/.cxx/Debug/xxx/x86或armeabi-v7a/CMakeFiles/3.18.1-g262b901-dirty/CMakeCCompiler.cmake
message(${CMAKE_SOURCE_DIR})
#这个 ${ANDROID_ABI} 其实是 armeabi-v7a 但是并不能直接这样写, 因为模拟器是 x86,
#所以要通过 ${ANDROID_ABI} 来知道当前的编译平台版本
message(${ANDROID_ABI})

#1. 导入头文件
include_directories(${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/include)

message("--------david2-----------")
message(${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/include)

#2. 导入库文件
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/lib")

#将当前目录的所有 cpp 文件全部添加到 cpp_source 变量
file(GLOB cpp_source  *.cpp)
add_library(
        x264rtmpmaniu
        SHARED
        ${cpp_source}
)

target_link_libraries(
        x264rtmpmaniu
        x264            #3. 将库的名称写到这里       libx264.a 去掉 lib 和 .a 剩下的 x264 就是库名
        log
)