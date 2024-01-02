#include <jni.h>
#include <string>
#include "VideoChannel.h"
#include "JavaCallHelper.h"
#include "librtmp/rtmp.h"
#include "safe_queue.h"
#include "maniulog.h"
SafeQueue<RTMPPacket *> packets;
int isStart = 0;
JavaCallHelper *helper = 0;
VideoChannel *videoChannel;
JavaVM *javaVM = 0;
JNIEnv* _env = NULL;
pthread_t pid;
uint32_t start_time;
//队列存放的入口
void callback(RTMPPacket *packet) {
    if (packet) {
        packet->m_nTimeStamp = RTMP_GetTime() - start_time;
        packets.push(packet);
    }
}
void releasePackets(RTMPPacket *&packet) {
    if (packet) {
        RTMPPacket_Free(packet);
        delete packet;
        packet = 0;
    }
}
void *start(void *args) {
    char *url = static_cast<char *>(args);

    RTMP *rtmp = 0;
    do {
        rtmp = RTMP_Alloc();
        if (!rtmp) {
            LOGE("rtmp创建失败");
            break;
        }
        RTMP_Init(rtmp);
        //设置超时时间 5s
        rtmp->Link.timeout = 5;
        int ret = RTMP_SetupURL(rtmp, url);
        if (!ret) {
            LOGE("rtmp设置地址失败:%s", url);
            break;
        }
        //开启输出模式
        RTMP_EnableWrite(rtmp);
        ret = RTMP_Connect(rtmp, 0);
        if (!ret) {
            LOGE("rtmp连接地址失败:%s", url);
            break;
        }
        ret = RTMP_ConnectStream(rtmp, 0);
        if (!ret) {
            LOGE("rtmp连接流失败:%s", url);
            break;
        }
        packets.setWork(1);
        RTMPPacket *packet = 0;
        //循环从队列取包 然后发送
//        硬解的java层，java层 能1 不能2
        //记录一个开始推流的时间
        start_time = RTMP_GetTime();
        while (isStart) {
            packets.pop(packet);
            if (!isStart) {
                break;
            }
            if (!packet) {
                continue;
            }
            // 给rtmp的流id
            packet->m_nInfoField2 = rtmp->m_stream_id;
            //发送包 1:加入队列发送
            ret = RTMP_SendPacket(rtmp, packet, 1);
            releasePackets(packet);
            if (!ret) {
                LOGE("发送数据失败");
                break;
            }

        }
        releasePackets(packet);
        } while (0);
    if (rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
    }
}
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved)
{
    jint result = -1;
    if (vm->GetEnv((void**) &_env, JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }
    javaVM = vm;
    return JNI_VERSION_1_4;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_maniu_x264rtmpmaniu_LivePusher_native_1init(JNIEnv *env, jobject thiz) {
    helper = new JavaCallHelper(javaVM, env, thiz);
     videoChannel=new VideoChannel ;
    packets= SafeQueue<RTMPPacket *>();
    videoChannel->setVideoCallback(callback);
    videoChannel->javaCallHelper = helper;

}
extern "C"
JNIEXPORT void JNICALL
Java_com_maniu_x264rtmpmaniu_LivePusher_native_1start(JNIEnv *env,
                                                      jobject thiz,
                                                      jstring path_) {

    if (isStart) {
        return;
    }
    const char *path = env->GetStringUTFChars(path_, 0);
//子线程

    char *url = new char[strlen(path) + 1];
    strcpy(url, path);
    isStart = 1;
    //启动线程
    pthread_create(&pid, 0, start, url);
    env->ReleaseStringUTFChars(path_, path);



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
    jbyte *data = env->GetByteArrayElements(data_, NULL);
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
    if (videoChannel) {
        delete (videoChannel);
        videoChannel = 0;
    }
}