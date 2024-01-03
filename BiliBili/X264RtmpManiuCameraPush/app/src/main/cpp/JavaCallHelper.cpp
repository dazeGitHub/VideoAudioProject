//
// Created by maniu on 2022/7/4.
//

#include <jni.h>
#include "JavaCallHelper.h"
#include "maniulog.h"

//jobj 是 LivePusher 的 Java 对象
JavaCallHelper::JavaCallHelper(JavaVM *_javaVM, JNIEnv *_env, jobject &_jobj) {
    this -> javaVM = _javaVM;
    this -> env = _env;
    jobj = _env->NewGlobalRef(_jobj);//将对象变为全局的

    jclass jclazz = _env -> GetObjectClass(jobj);
//  private void postData(byte[] data)      [B  表示参数是 byte 数组, V 表示无符号
    jmid_postData = _env -> GetMethodID(jclazz, "postData", "([B)V");
}

JavaCallHelper::~JavaCallHelper() {
//  不需要的时候释放
    env -> DeleteGlobalRef(jobj);
}

//知道   回调java   值
//在 JavaCallHelper.h 中为 thread 设置了默认值 thread = THREAD_MAIN, 所以 thread 实参可以不传
void JavaCallHelper::postH264(char *data, int length, int thread) {
    LOGE("--->  postH264 1   %p  length  %d", env, length);
    jbyteArray array = env->NewByteArray(length); //这里会报错, 暂时注释后面
    LOGE("--->  postH264 1.5   %p ",env);
//    env->SetByteArrayRegion(array, 0, length, reinterpret_cast<jbyte *>(data)); //将 c 数组转换为 java 数组
//
//    //如果是 Java 子线程
//    LOGE("--->  postH264 2  ");
//    if (thread == THREAD_CHILD) {
//        JNIEnv * jniEnv;
//        if (javaVM -> AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
//            return;
//        }
//        jniEnv -> CallVoidMethod(jobj, jmid_postData, array);
//        javaVM -> DetachCurrentThread();
//    } else{ //如果是 Java 主线程
//        env -> CallVoidMethod(jobj, jmid_postData, array);
//    }
//    LOGE("--->  postH264 3 ");
}
