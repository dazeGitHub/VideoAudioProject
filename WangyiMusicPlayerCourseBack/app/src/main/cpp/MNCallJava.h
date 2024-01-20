//
// Created by maniu on 2022/8/5.
//

#ifndef WANGYIMUSICPLAYER_MNCALLJAVA_H
#define WANGYIMUSICPLAYER_MNCALLJAVA_H
#include "jni.h"
#include <linux/stddef.h>
#include "AndroidLog.h"
#define MAIN_THREAD 0
#define CHILD_THREAD 1
//子线程     主线程

//native   处于子线程    主线程   能 1  不能2
class MNCallJava {
public:
    _JavaVM *javaVM = NULL;
    JNIEnv *jniEnv = NULL;
    jobject jobj;
    jmethodID jmid_parpared;
    jmethodID jmid_timeinfo;
    jmethodID jmid_load;
    jmethodID jmid_renderyuv;
public:
    MNCallJava(_JavaVM *javaVM, JNIEnv *env, jobject obj);
    void onCallParpared(int type);
    void onCallTimeInfo(int type, int curr, int total);
//加载中的状态
    void onCallLoad(int type, bool load);
//    新方式     渲染native    opemgl
    void onCallRenderYUV(int width, int height, uint8_t *fy, uint8_t *fu, uint8_t *fv);
};


#endif //WANGYIMUSICPLAYER_MNCALLJAVA_H
