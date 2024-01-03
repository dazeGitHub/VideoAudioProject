//
// Created by maniu on 2022/7/4.
//

#ifndef X264RTMPMANIU_JAVACALLHELPER_H
#define X264RTMPMANIU_JAVACALLHELPER_H

//标记线程 因为子线程需要attach
#define THREAD_MAIN 1
#define THREAD_CHILD 2

class JavaCallHelper {
public:
    JavaCallHelper(JavaVM *_javaVM, JNIEnv *_env, jobject &_jobj);
    ~JavaCallHelper();
//  参数  叫做是不是 主线程 直线, thread 是线程 id, 如果是主线程就直接反射, 如果是子线程需要通过 Jvm 引擎再反射
    void postH264(char * data,int length, int thread = THREAD_MAIN);
public:
//  jmethodID  回调
    JavaVM *javaVM;
    JNIEnv *env = NULL;
    jobject jobj;
//  回调分为两种 : 主线程回调, 子线程回调
    jmethodID jmid_postData;
};
#endif //X264RTMPMANIU_JAVACALLHELPER_H
