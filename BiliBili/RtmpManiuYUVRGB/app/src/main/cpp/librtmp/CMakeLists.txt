set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNO_CRYPTO"  )
file(GLOB rtmp_source *.c)
add_library(rtmp111 STATIC ${rtmp_source})

#apk    librtmp.a