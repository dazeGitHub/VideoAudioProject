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
//     参数  叫做是不是 主线程   直线
    void postH264(char *data,int length, int thread = THREAD_MAIN);
public:
//    jmethodID  回调
    JavaVM *javaVM;
    JNIEnv *env = NULL;
    jobject jobj;
//主线程回调
//子线程回调
    jmethodID jmid_postData;
};



#endif //X264RTMPMANIU_JAVACALLHELPER_H
