
#ifndef MYMUSIC_WLCALLJAVA_H
#define MYMUSIC_WLCALLJAVA_H

#include "jni.h"
#include <linux/stddef.h>
#include "AndroidLog.h"

#define MAIN_THREAD 0
#define CHILD_THREAD 1


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
    MNCallJava(_JavaVM *javaVM, JNIEnv *env, jobject *obj);
    ~MNCallJava();

    void onCallParpared(int type);
    void onCallTimeInfo(int type, int curr, int total);
    void onCallLoad(int type, bool load);
    void onCallRenderYUV(int width, int height, uint8_t *fy, uint8_t *fu, uint8_t *fv);

};


#endif //MYMUSIC_WLCALLJAVA_H
