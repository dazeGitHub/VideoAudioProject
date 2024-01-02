//
// Created by maniu on 2022/7/4.
//

#include <jni.h>
#include "JavaCallHelper.h"
#include "maniulog.h"

JavaCallHelper::JavaCallHelper(JavaVM *_javaVM, JNIEnv *_env, jobject &_jobj) {
    this->javaVM = _javaVM;
    this->env = _env;
    jobj = _env->NewGlobalRef(_jobj);

    jclass jclazz = _env->GetObjectClass(jobj);
    jmid_postData = _env->GetMethodID(jclazz, "postData", "([B)V");
}

JavaCallHelper::~JavaCallHelper() {
//    不需要的时候
//    env->DeleteGlobalRef(jobj);
}
//知道   回调java   值
void JavaCallHelper::postH264(char *data, int length, int thread) {
    LOGE("--->  postH264 1   %p  length  %d", env, length);
    env->NewByteArray(length);
    LOGE("--->  postH264 1.5   %p ",env);
//    env->SetByteArrayRegion(array, 0, length, reinterpret_cast<jbyte *>(data));
//子线程
//    LOGE("--->  postH264 2  ");
//    if (thread == THREAD_CHILD) {
//        JNIEnv *jniEnv;
//        if (javaVM->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
//            return;
//        }
//        jniEnv->CallVoidMethod(jobj, jmid_postData,array);
//        javaVM->DetachCurrentThread();
//    } else{
//        //    main线程
//        env->CallVoidMethod(jobj, jmid_postData, array);
//    }
    LOGE("--->  postH264 3 ");
}
