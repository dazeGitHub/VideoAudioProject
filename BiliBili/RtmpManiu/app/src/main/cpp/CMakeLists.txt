cmake_minimum_required(VERSION 3.18.1)

project("rtmpmaniu")

#定义变量一定要使用 GLOB
#定义一个全局变量 david 代表当前目录下 所有的 cpp 文件
file(GLOB david *.cpp)
add_subdirectory(librtmp)

add_library(
        #编译的 so 库的名字就是 rtmpmaniu, 那么反编译 app-debug.apk 中 lib/arm64-v8a 中就会有 librtmpmaniu.so
        rtmpmaniu
        #SHARED 表示动态库(以 so 结尾), Static 表示静态库(以 a 结尾)
        SHARED
        #这里表示要编译的源文件
        #native-lib.cpp
        #./librtmp/rtmp.c
        #直接引用 david 变量
        ${david}
)

#系统目录  系统环境变量     log-lib 变量
#如果要引入其他库, 那么配置 find_library()
#表示在 系统目录(系统环境变量) 中查找 log 库, 路径为 ndk目录/sysroot/usr/lib/aarch64-linux-android/22/liblog.so
#然后赋值给 log-lib 这个变量
#同理, 处理 Bitmap 需要引入 jnigraphics 库, 即 libjnigraphics.so  去掉前面的 lib 和 后面的 so
find_library(
        log-lib #这是变量
        log #这是库
)

target_link_libraries(
        rtmpmaniu
        ${log-lib} #将 log-lib 这个变量代表的 log 库添加到 rtmpmaniu 工程里面
        rtmp111 #直接引用 librtmp/CMakeLists.txt 中的 rtmp111 静态库
)

#这样写也可以, 表示从系统变量中的 log 库添加到 rtmpmaniu 工程里面
#target_link_libraries(
#        rtmpmaniu
#        log
#)

#这样写不行, 会报错 jnigraphics1111 找不到, 因为系统目录中就没有这个库
#target_link_libraries(
#        rtmpmaniu
#        jnigraphics1111
#)