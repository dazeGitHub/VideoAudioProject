
cmake_minimum_required(VERSION 3.4.1)
add_subdirectory(librtmp)
add_library( # Sets the name of the library.
        native-lib
        SHARED

        # Provides a relative path to your source file(s).
        native-lib.cpp )
find_library( # Sets the name of the path variable.
        log-lib
        log )

target_link_libraries( # Specifies the target library.
        native-lib

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib}
        rtmp)