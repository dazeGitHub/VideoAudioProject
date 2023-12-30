#include <jni.h>
#include <string>
extern "C"
{
    #include  "librtmp/rtmp.h"
}
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,"David",__VA_ARGS__)

typedef struct {
    int8_t *sps;
    int16_t sps_len;

    int16_t pps_len;
    int8_t *pps;

    RTMP *rtmp;
} Live;
Live *live = nullptr;

void prepareVideo(int8_t *data, int len, Live *live) {
    for (int i = 0; i < len; i++) {
        //0x00 0x00 0x00 0x01
        if (i + 4 < len) {
            if (data[i] == 0x00 && data[i + 1] == 0x00
                && data[i + 2] == 0x00
                && data[i + 3] == 0x01) {
                //0x00 0x00 0x00 0x01 7 sps 0x00 0x00 0x00 0x01 8 pps
                //将sps pps分开
                //找到pps
                if (data[i + 4]  == 0x68) {
                    //去掉界定符
                    live->sps_len = i - 4;
                    live->sps = static_cast<int8_t *>(malloc(live->sps_len));
                    memcpy(live->sps, data + 4, live->sps_len);

                    live->pps_len = len - (4 + live->sps_len) - 4;
                    live->pps = static_cast<int8_t *>(malloc(live->pps_len));
                    memcpy(live->pps, data + 4 + live->sps_len + 4, live->pps_len);
                    LOGI("sps:%d pps:%d", live->sps_len, live->pps_len);
                    break;
                }
            }
        }
    }
}

//第一次输出sps pps
//00 00 00 01   67  42C0298D680B40A1A01E1108D4   00   00  00  01    68CE01A835C8

//void prepareVideo(int8_t *data, int len, Live *live) {
//
//    for (int i = 0; i < len; i++) {
//        if (data[i] == 0x00 && data[i + 1] == 0x00
//            && data[i + 2] == 0x00
//            && data[i + 3] == 0x01&&  data[i + 4] == 0x68){
//            LOGI("david--------------> 找到sps pps  type " );
////            计算sps长度
//            live->sps_len = i - 4;
////            根据长度   初始化容器
//            live->sps = static_cast<int8_t *>(malloc(live->sps_len));
////            copy sps内存到容器
//            memcpy(live->sps, data + 4, live->sps_len);
//
////PPS
//            live->pps_len = len - i - 4;
//            live->pps = static_cast<int8_t *>(malloc(live->pps_len));
//            memcpy(live->pps, data +i + 4, live->pps_len);
//            break;
//        }
//    }
//}

RTMPPacket * createVideoPackageSPS_PPS(Live * live) {
        RTMPPacket * packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
//      每个数据包 肯定 不一样
//      RTMP 视频包数据 那张图 sps 与 pps 除了 sps 长度和 pps 长度外所有值的和是 16
        int body_size = 16 + live->sps_len + live->pps_len;
//      I  P  B
//      sps pps
        RTMPPacket_Alloc(packet, body_size); //初始化 body

        int i = 0;
        packet->m_body[i++] = 0x17;
        packet->m_body[i++] = 0x00;
        //CompositionTime
        packet->m_body[i++] = 0x00;
        packet->m_body[i++] = 0x00;
        packet->m_body[i++] = 0x00;

//      版本
        //AVC sequence header
        packet->m_body[i++] = 0x01;

//      编码规格
        packet->m_body[i++] = live->sps[1]; //profile 如baseline、main、 high
        packet->m_body[i++] = live->sps[2]; //profile_compatibility 兼容性
        packet->m_body[i++] = live->sps[3]; //level 代表最大支持码流范围
//      固定写法
        packet->m_body[i++] = 0xFF; //几个字节用来表示 NALU 的长度
        packet->m_body[i++] = 0xE1; //SPS 个数

//      sps 长度  2个字节
//      live->sps_len 是 int16_t 类型的, 占用 2个字节, 所以要拆开存储到 m_body[i] 里面
//      和 0xFF 与后 sps_len >> 8 就从 int 类型变为字节类型
        packet->m_body[i++] = (live->sps_len >> 8) & 0xFF; //先取高 8 位
        packet->m_body[i++] = live->sps_len & 0xFF; //再取低 8 位

//      sps 内容
//      67  42C0298D680B40A1A01E1108D4
        memcpy(&packet->m_body[i], live->sps, live->sps_len);
//      拷贝 i++后面去 方便后面继续赋值
        i += live->sps_len;

//      pps 个数就是 1
        packet->m_body[i++] = 0x01;

        //pps 长度
        packet->m_body[i++] = (live->pps_len >> 8) & 0xff;
        packet->m_body[i++] = live->pps_len & 0xff;
        memcpy(&packet->m_body[i], live->pps, live->pps_len);

//      数据包拼接成功    下面的配置用来做同步
        packet->m_packetType = RTMP_PACKET_TYPE_VIDEO; //配置数据包类型是视频包
        packet->m_nBodySize = body_size; //
        packet->m_nChannel = 0x04;
        packet->m_hasAbsTimestamp = 0; //0 表示不使用绝对时间, 而是使用相对时间
//      系统赋值, 给每一帧设置时间戳
        packet->m_nTimeStamp = 0; //这个是相对于主播的时间
//      数据包大小  给服务器看的
        packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
//      固定传的
        packet->m_nInfoField2 = live->rtmp->m_stream_id;
        return packet;
//      昨天 周一
}

//将发送方法写成一个单独函数
int sendPacket(RTMPPacket *packet) {
    int r = RTMP_SendPacket(live->rtmp, packet, 1);
    RTMPPacket_Free(packet);//这个是释放 packet 中的 body
    free(packet);//这个是释放 packet
    return r;
}

//I 帧的数据 :
//00 00 00 01 65 B80004059FFFFF0451400040BFC7000106A8E00053FC6C600FFF0044CC4C7567E
//该方法用来处理 关键帧 和 非关键帧
RTMPPacket * createVideoPackageFrame(int8_t *buf, int len, const long tms, Live *live) {
//  不需要分隔符, 服务器转发时会再加分隔符
    buf += 4;
    len -= 4;
    RTMPPacket * packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    int body_size = 9 + len; //RTMP 视频包数据  关键帧那行 h264裸数据 前面正好 9 个字节, len 是 h264 I 帧 数据流的长度
    RTMPPacket_Alloc(packet, body_size); //初始化 body

    //关键帧和非关键帧的唯一区别就是这里
    if (buf[0] == 0x65) {     //如果是 I 帧 (关键帧)
        packet->m_body[0] = 0x17;
    } else{
        packet->m_body[0] = 0x27;
    }
    packet->m_body[1] = 0x01; //第二个字节就是 0x01
    packet->m_body[2] = 0x00;
    packet->m_body[3] = 0x00;
    packet->m_body[4] = 0x00;
    //编码规格  将四个字节的 len 分别存储在 m_body[5], m_body[6], m_body[7], m_body[8]
    packet->m_body[5] = (len >> 24) & 0xff;
    packet->m_body[6] = (len >> 16) & 0xff;
    packet->m_body[7] = (len >> 8) & 0xff;
    packet->m_body[8] = (len) & 0xff;
    //数据
    memcpy(&packet->m_body[9], buf, len); //SPS 个数

//  拼接成功       配置 直播怎么做到 配置, 下面的内容参考  createVideoPackageSPS_PPS() 方法
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = body_size;
    //修改这个值可以推多路视频, 如果是摄像头, 可以设置该值为 0x05, 甚至是 6 路, 最大限制来自服务器的转发能力。
    packet->m_nChannel = 0x04;
    packet->m_hasAbsTimestamp = 0;
//  有时间戳就赋值时间戳
    packet->m_nTimeStamp = tms;
//  数据包大小  给服务器看的
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = live->rtmp->m_stream_id;
    return packet;

}
//0000000141B80004059FFFFF0451400040BFC7000106A8E00053FC6C600FFF0044CC4C7567E
//RTMPPacket *createVideoPackage1(int8_t *buf, int len, const long tms, Live *live) {
//    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
//    int body_size = 9 +len;
//    RTMPPacket_Alloc(packet, body_size);
//    packet->m_body[0] = 0x27;
//    packet->m_body[1] = 0x01;
//    packet->m_body[2] = 0x00;
//    packet->m_body[3] = 0x00;
//    packet->m_body[4] = 0x00;
//    //长度
//    packet->m_body[5] = (len >> 24) & 0xff;
//    packet->m_body[6] = (len >> 16) & 0xff;
//    packet->m_body[7] = (len >> 8) & 0xff;
//    packet->m_body[8] = (len) & 0xff;
//    //数据
//    memcpy(&packet->m_body[9], buf, len);
//
//
////        拼接成功       配置 直播怎么做到 配置
//    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
//    packet->m_nBodySize = body_size;
//    packet->m_nChannel = 0x04;
//    packet->m_hasAbsTimestamp = 0;
////        系统赋值
//    packet->m_nTimeStamp = tms;
////        数据包大小  给服务器看的
//    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
//    packet->m_nInfoField2 = live->rtmp->m_stream_id;
//    return packet;
//
//}
//00 00 00 01   67  42C0298D680B40A1A01E1108D4   00   00  00  01    68CE01A835C8
//0000000165B80004059FFFFF0451400040BFC7000106A8E00053FC6C600FFF0044CC4C7567E
//int sendVideo(int8_t *buf, int len, long tms) {
//    //sps pps
//    int ret;
//    int type = (buf[4] & 0x1F);
//    LOGI("david-------------->type  %d",type);
//    if (buf[4] == 0x67) {
//        LOGI("david-------------->sps  type  %d", (!live->pps || !live->sps));
//        if (live && ((!live->pps) || (!live->sps))) {
//            LOGI("david-------------->sps prepareVideo");
//            prepareVideo(buf, len, live);
//        }
//        return ret;
//    }
////    I帧
//    if (buf[4] == 0x65) {
////        发两帧  一帧sps  一帧 I帧
////拼接Packet
//        RTMPPacket *packet = createVideoPackage(live);
//        sendPacket(packet);
//
//    }
//
//    //拼接I帧
//    RTMPPacket *packet1 = createVideoPackage(buf, len, tms, live);
////发送I帧
//    sendPacket(packet1);
//    LOGI("----->发送  ");
//    return ret;
//}

RTMPPacket *createAudioPacket(int8_t *buf, const int len, const int type, const long tms,
                              Live *live) {
    int body_size = len + 2;
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    RTMPPacket_Alloc(packet, body_size);
    packet->m_body[0] = 0xAF;
    if (type == 1) { //如果是解码数据  RTMP_PACKET_TYPE_AUDIO_HEAD  1
        packet->m_body[1] = 0x00;
    } else{ //如果是音频数据信息 RTMP_PACKET_TYPE_AUDIO_DATA  2
        packet->m_body[1] = 0x01;
    }
    memcpy(&packet->m_body[2], buf, len);

    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nChannel = 0x05; //绝对不要写 0x04
    packet->m_nBodySize = body_size;
    packet->m_nTimeStamp = tms;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = live->rtmp->m_stream_id;
    return packet;

}
int sendAudio(int8_t *buf, int len, int type, int tms) {
//    创建音频包   如何组装音频包
    RTMPPacket * packet = createAudioPacket(buf, len, type, tms, live);
    int ret = sendPacket(packet);
    return ret;
}


int sendVideo(int8_t * buf, int len, long tms) {
    int ret;
    if (buf[4] == 0x67) {//sps pps
        if (live && (!live->pps || !live->sps)) {
            prepareVideo(buf, len, live);
        }
    } else {
        //如果是 I 帧, 发送完 SPS 和 PPS 后再发送 I 帧, 否则不发送 SPS 和 PPS 直接发送非 I 帧
        if (buf[4] == 0x65) {//关键帧
            RTMPPacket * packet = createVideoPackageSPS_PPS(live);
            //这里是发送了 sps 和 pps, 所以 createVideoPackage(live) 只使用了 live 没有使用 buf
            if (!(ret = sendPacket(packet))) {

            }
        }
        //拼接 I 或 非 I 帧
        RTMPPacket * packet = createVideoPackageFrame(buf, len, tms, live);
        //发送 I 或 非 I 帧
        ret = sendPacket(packet);
    }
    return ret;
}

extern "C"
JNIEXPORT jboolean
Java_com_maniu_rtmpmaniu_ScreenLive_sendData(JNIEnv *env, jobject thiz, jbyteArray data_,
                                                 jint len, jlong tms, jint type) {
    int ret;
    jbyte *data = env->GetByteArrayElements(data_, NULL);
    switch (type) {
        case 0: //video
            ret = sendVideo(data, len, tms);
            break;
        default: //audio
            ret = sendAudio(data, len, type, tms);
            LOGI("send Audio  lenght :%d", len);
            break;
    }
    env->ReleaseByteArrayElements(data_, data, 0);
    return ret;
}
//手机硬件   手机 模拟器  H265
extern "C"
JNIEXPORT jboolean
Java_com_maniu_rtmpmaniu_ScreenLive_connect(JNIEnv *env, jobject thiz, jstring url_) {
//能1  不能2 MK  2.3
    const char *url = env->GetStringUTFChars(url_, 0);
    int ret;
    do {
        live = (Live*)malloc(sizeof(Live));
        memset(live, 0, sizeof(Live));
        live->rtmp = RTMP_Alloc();
        RTMP_Init(live->rtmp);
        live->rtmp->Link.timeout = 10;
        LOGI("connect %s", url);
        if (!(ret = RTMP_SetupURL(live->rtmp, (char*)url))) break;
        RTMP_EnableWrite(live->rtmp);
        LOGI("RTMP_Connect");
        if (!(ret = RTMP_Connect(live->rtmp, 0))) break;
        LOGI("RTMP_ConnectStream ");
        if (!(ret = RTMP_ConnectStream(live->rtmp, 0))) break;
        LOGI("connect success");
    } while (0);
    if (!ret && live) {
        free(live);
        live = nullptr;
    }

    env->ReleaseStringUTFChars(url_, url);
    return ret;
}
