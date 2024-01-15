#include <jni.h>
#include <string>
#include "MNFFmpeg.h"
#include "MNCallJava.h"

MNFFmpeg *ffmpeg = NULL;
MNCallJava *callJava = NULL;
_JavaVM *javaVM = NULL;
MNPlaystatus *playstatus = NULL;
extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
    jint result = -1;
    javaVM = vm;
    JNIEnv *env;
    if(vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK)
    {

        return result;
    }
    return JNI_VERSION_1_4;

}



extern "C"
JNIEXPORT void JNICALL
Java_com_maniu_wangyimusicplayer_service_MNPlayer_n_1parpared(JNIEnv *env, jobject instance, jstring source_) {
    const char *source = env->GetStringUTFChars(source_, 0);
    if(ffmpeg == NULL)
    {
        if(callJava == NULL)
        {
            LOGE("------------->1");
            callJava = new MNCallJava(javaVM, env, instance);
        }
        LOGE("------------->3");
        playstatus = new MNPlaystatus();
        ffmpeg = new MNFFmpeg(playstatus,callJava,source);
        ffmpeg->callJava = callJava;
        ffmpeg->parpared();
        LOGE("------------->11");
    }
//    env->ReleaseStringUTFChars(source_, source);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_maniu_wangyimusicplayer_service_MNPlayer_n_1start(JNIEnv *env, jobject thiz) {
    if(ffmpeg != NULL)
    {
        ffmpeg->start();
    }


}
extern "C"
JNIEXPORT void JNICALL
Java_com_maniu_wangyimusicplayer_service_MNPlayer_n_1pause(JNIEnv *env, jobject thiz) {
    if(ffmpeg != NULL)
    {
        ffmpeg->pause();
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_maniu_wangyimusicplayer_service_MNPlayer_n_1seek(JNIEnv *env, jobject thiz, jint secds) {
    LOGE("最开始%d  ", secds);
    if(ffmpeg != NULL)
    {
        ffmpeg->seek(secds);
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_maniu_wangyimusicplayer_service_MNPlayer_n_1resume(JNIEnv *env, jobject thiz) {
    if(ffmpeg != NULL)
    {
        ffmpeg->resume();
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_maniu_wangyimusicplayer_service_MNPlayer_n_1mute(JNIEnv *env, jobject thiz, jint mute) {
    if(ffmpeg != NULL)
    {
        ffmpeg->setMute(mute);
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_maniu_wangyimusicplayer_service_MNPlayer_n_1volume(JNIEnv *env, jobject thiz,
                                                            jint percent) {
    // TODO: implement n_volume()
}