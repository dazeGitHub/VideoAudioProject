set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNO_CRYPTO"  )

#定义一个全局变量 rtmp_source 代表当前目录下的所有 c 文件
file(GLOB rtmp_source *.c)

#定义一个静态库 rtmp111, 引用 rtmp_source 中的所有文件
add_library(rtmp111 STATIC ${rtmp_source})

#在 apk 中不会出现静态库 librtmp.a, 静态库最终都会打入到动态库里