#include <jni.h>
#include <string>

//如果是 c 函数才需要 extern "C"
#include "VideoChannel.h"

VideoChannel * videoChannel;
extern "C" JNIEXPORT jstring JNICALL
Java_com_maniu_x264rtmpmaniu_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_maniu_x264rtmpmaniu_LivePusher_native_1init(JNIEnv *env, jobject thiz) {
//   cpp 中创建对象的三种方式 :
//   1. VideoChannel * videoChannel 创建后就不用管释放了, 系统会释放
//   2. VideoChannel * videoChannel = new VideoChannel
//   3. VideoChannel * videoChannel = new VideoChannel()        加不加括号都行
     videoChannel = new VideoChannel ;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_maniu_x264rtmpmaniu_LivePusher_native_1start(JNIEnv *env, jobject thiz, jstring path) {

}

extern "C"
JNIEXPORT void JNICALL
Java_com_maniu_x264rtmpmaniu_LivePusher_native_1setVideoEncInfo(JNIEnv *env, jobject thiz,
                                                                jint width, jint height, jint fps,
                                                                jint bitrate) {
    if (videoChannel) {
        videoChannel->setVideoEncInfo(width, height, fps, bitrate);
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_maniu_x264rtmpmaniu_LivePusher_native_1pushVideo(JNIEnv *env, jobject thiz,
                                                          jbyteArray data_) {
    if (!videoChannel) {
        return;
    }
    jbyte *data = env->GetByteArrayElements(data_, NULL); //将 java 中的数组转换为 c 中的数组
    videoChannel->encodeData(data);
    env->ReleaseByteArrayElements(data_, data, 0);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_maniu_x264rtmpmaniu_LivePusher_native_1stop(JNIEnv *env, jobject thiz) {

}
extern "C"
JNIEXPORT void JNICALL
Java_com_maniu_x264rtmpmaniu_LivePusher_native_1release(JNIEnv *env, jobject thiz) {
    //释放 videoChannel 对象
    if (videoChannel) {
        delete (videoChannel);
        videoChannel = 0;
    }
}