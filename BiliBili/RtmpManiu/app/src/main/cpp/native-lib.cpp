#include <jni.h>
#include <string>

//当前是 cpp 文件, 如果要引入 c 文件, 那么需要添加 extern "C"
extern "C"
{
    #include  "librtmp/rtmp.h"
}
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,"David",__VA_ARGS__)

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_maniu_rtmpmaniu_ScreenLive_connect(JNIEnv *env, jobject thiz, jstring url_) {
//能1  不能2 MK  2.3
    const char * url = env->GetStringUTFChars(url_, 0); //先将 jstring 转换为 c 中的字符串
    int ret;
    //实例化对象
    RTMP * rtmp = RTMP_Alloc();
    RTMP_Init(rtmp);
    rtmp -> Link.timeout = 10;
    ret = RTMP_SetupURL(rtmp, (char*) url); //成功返回1
    if (ret == TRUE) {
        LOGI("RTMP_SetupURL");
    }
    RTMP_EnableWrite(rtmp);
    LOGI("RTMP_EnableWrite");

    ret = RTMP_Connect(rtmp, 0);
    if (ret == TRUE) {
        LOGI("RTMP_Connect");
    }
    ret = RTMP_ConnectStream(rtmp, 0);
    if (ret == TRUE) {
        LOGI("connect success");
    }
    env -> ReleaseStringUTFChars(url_, url);//释放 c 中的指针 url
    return ret;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_maniu_rtmpmaniu_ScreenLive_testNativeCrash(JNIEnv *env, jobject thiz) {
    int b = 3, c = 0;
    int a = b / c;
}