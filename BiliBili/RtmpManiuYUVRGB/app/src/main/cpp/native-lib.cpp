#include <jni.h>
#include <string>
extern "C"
{
    #include  "librtmp/rtmp.h"
}

typedef struct {
    //使用 指针 + 长度 来表示 sps 或 pps
    int8_t * sps;
    int16_t sps_len;

    int16_t pps_len;
    int8_t * pps;
} Live;
Live * live = nullptr;

//目的是将 sps 和 pps 解析到 live 容器中
//第一次输出sps pps
//00 00 00 01   67  42C0298D680B40A1A01E1108D4   00   00  00  01    68CE01A835C8
void prepareVideo(int8_t *data, int len, Live *live) {

    for (int i = 0; i < len; i++) {

        if (data[i] == 0x00 && data[i + 1] == 0x00
            && data[i + 2] == 0x00
            && data[i + 3] == 0x01 &&  data[i + 4] == 0x68){

//          计算sps长度
            live-> sps_len = i - 4;
//          根据长度   初始化容器, 开辟 live->sps_len 长度
            live-> sps = static_cast<int8_t *>(malloc(live->sps_len));
//          copy sps 内存到容器
            memcpy(live->sps, data + 4, live->sps_len);

//          PPS
            live-> pps_len = len - i - 4;
            live-> pps = static_cast<int8_t *>(malloc(live->pps_len));
            memcpy(live->pps, data + i + 4, live->pps_len);
            break;
        }
    }
}

RTMPPacket *createVideoPackage(Live *live) {
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
}

//参考 RTMP 视频包数据那张图
//1. 先将 sps 和 pps 缓存到一个地方
//这是 SPS : 00 00 00 01   67  42C0298D680B40A1A01E1108D4         SPS 长度是 index - 4
//这是 PPS : 00 00 00 01   68  CE01A835C8
//还可以知道 SPS 和 PPS 的整个长度为 len                              PPS 长度是 len - index - 4,
int sendVideo(int8_t *buf, int len, long tms) {
    //sps pps
    int ret;
    int type = (buf[4] & 0x1F);//buf[4] 是 67
    if (type == 7) { //判断类型是否是 sps 或 pps
        if (live && (!live->pps || !live->sps)) { //如果 live 不为 null 并且 live 中 pps 或 sps 有一个为 null
            prepareVideo(buf, len, live);
        }
    }

//  I帧    00 00 00 01 65 B80004059FFFFF0451400040BFC7000106A8E00053FC6C600FFF0044CC4C7567E
    if (type == 0x5) {
//  发两帧  一帧sps  一帧 I帧
//  拼接Packet
        RTMPPacket * packet = createVideoPackage(live);

//  sps  pps    这里没写完
//        RTMP_SendPacket()
//  I帧
//        RTMP_SendPacket()
    }

    return ret;
}

//推流方法, 包含 长度 len, 时间戳 tms, 类型 type
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,"David",__VA_ARGS__)
extern "C"
JNIEXPORT jboolean
Java_com_maniu_rtmpmaniu_ScreenLive_sendData(JNIEnv *env, jobject thiz, jbyteArray data_,
                                                 jint len, jlong tms,jint type) {
    int ret;
    jbyte * data = env->GetByteArrayElements(data_, NULL); //先将 java 中的字节数组转换为 c 中的字节数组指针
    ret = sendVideo(data, len, tms);
    env->ReleaseByteArrayElements(data_, data, 0);
    return ret;
}

extern "C"
JNIEXPORT jboolean
Java_com_maniu_rtmpmaniu_ScreenLive_connect(JNIEnv *env, jobject thiz, jstring url_) {
//能1  不能2 MK  2.3
    const char *url = env->GetStringUTFChars(url_, 0);
    int ret;
    live = (Live*)malloc(sizeof(Live));
//实例化对象
    RTMP * rtmp = RTMP_Alloc();
    RTMP_Init(rtmp);
    rtmp->Link.timeout = 10;
    ret =RTMP_SetupURL(rtmp, (char*)url);
    if (ret == TRUE) {
        LOGI("RTMP_SetupURL");
    }
//    int b = 3, c = 0;
//    int a = b / c;

    RTMP_EnableWrite(rtmp);
    LOGI("RTMP_EnableWrite");
    ret = RTMP_Connect(rtmp, 0);
    if (ret == TRUE) {
        LOGI("RTMP_Connect ");
    }
     ret = RTMP_ConnectStream(rtmp, 0);
    if (ret == TRUE) {
        LOGI("connect success");
    }
    env->ReleaseStringUTFChars(url_, url);
    return ret;
}