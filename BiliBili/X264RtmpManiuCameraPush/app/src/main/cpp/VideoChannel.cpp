//
// Created by maniu on 2022/6/28.
//

#include <cstring>
#include "VideoChannel.h"
#include "maniulog.h"

VideoChannel::VideoChannel() {

}

void VideoChannel::setVideoEncInfo(int width, int height, int fps, int bitrate) {

//    初始化 x264
    mWidth = width;
    mHeight = height;
    LOGE("初始化setVideoEncInfo  mWidth %d  height %d ",mWidth,mHeight);
    ySize = width * height;
    uvSize = ySize / 4;
    mFps = fps;
    mBitrate = bitrate;
//    定义参数 隐式声明
    x264_param_t  param ;
    //    参数赋值  ultrafast    编码器 速度   直播  越快 1  越慢2  zerolatency   编码质量   电影类型直播选用 zerolatency
    x264_param_default_preset(&param, "ultrafast", "zerolatency");
//    编码登记
    param.i_level_idc = 32;
//    选取显示格式
    param.i_csp = X264_CSP_I420;
    param.i_width = width;
    param.i_height = height;
    //    B帧
//    mediacodec b能编码   是1    小米11
    param.i_bframe = 0;
//折中    cpu   突发情况   ABR 平均
    param.rc.i_rc_method = X264_RC_ABR;
//k为单位  I帧  的比特
    param.rc.i_bitrate= bitrate / 1024;


    //帧率   1s/25帧
//    分母
    param.i_fps_num = fps;
//    分子
    param.i_fps_den = 1;

//    1s/帧率 =时间

//帧率     1s/时间

//    分母
    param.i_timebase_den=param.i_fps_num;
//    分子
    param.i_timebase_num = param.i_fps_den;

    param.b_vfr_input= 0;
//    秒开  i帧 2500   1s
    param.i_keyint_max = 2 * fps;

// sps pps 是不是重复输出 1     0 输出一次
    param.b_repeat_headers = 1;

    //多线程
    param.i_threads = 1;

    x264_param_apply_profile(&param, "baseline");
    videoCodec = x264_encoder_open(&param);
    pic_in = new x264_picture_t;
    x264_picture_alloc(pic_in, X264_CSP_I420, width, height);
//    初始化搞定了  简单
    LOGE("初始化setVideoEncInfo2 ");
}

void VideoChannel::sendSpsPps(uint8_t *sps, uint8_t *pps, int sps_len,
                              int pps_len) {
    RTMPPacket *packet = new RTMPPacket;
    int bodysize = 13 + sps_len + 3 + pps_len;
    RTMPPacket_Alloc(packet, bodysize);
    int i = 0;
    //固定头
    packet->m_body[i++] = 0x17;
    //类型
    packet->m_body[i++] = 0x00;
    //composition time 0x000000
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;

    //版本
    packet->m_body[i++] = 0x01;
    //编码规格
    packet->m_body[i++] = sps[1];
    packet->m_body[i++] = sps[2];
    packet->m_body[i++] = sps[3];
    packet->m_body[i++] = 0xFF;

    //整个sps
    packet->m_body[i++] = 0xE1;
    //sps长度
    packet->m_body[i++] = (sps_len >> 8) & 0xff;
    packet->m_body[i++] = sps_len & 0xff;
    memcpy(&packet->m_body[i], sps, sps_len);
    i += sps_len;

    //pps
    packet->m_body[i++] = 0x01;
    packet->m_body[i++] = (pps_len >> 8) & 0xff;
    packet->m_body[i++] = (pps_len) & 0xff;
    memcpy(&packet->m_body[i], pps, pps_len);


    //视频
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = bodysize;
    //随意分配一个管道（尽量避开rtmp.c中使用的）
    packet->m_nChannel = 10;
    //sps pps没有时间戳
    packet->m_nTimeStamp = 0;
    //不使用绝对时间
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    LOGE("  callback %p ",callback);
    callback(packet);
}

VideoChannel::~VideoChannel() {

}

void VideoChannel::sendFrame(int type, int payload, uint8_t *p_payload) {
    //去掉 00 00 00 01 / 00 00 01
    if (p_payload[2] == 0x00){
        payload -= 4;
        p_payload += 4;
    } else if(p_payload[2] == 0x01){
        payload -= 3;
        p_payload += 3;
    }
    RTMPPacket *packet = new RTMPPacket;
    int bodysize = 9 + payload;
    RTMPPacket_Alloc(packet, bodysize);
    RTMPPacket_Reset(packet);
//    int type = payload[0] & 0x1f;
    packet->m_body[0] = 0x27;
    //关键帧
    if (type == NAL_SLICE_IDR) {
        LOGE("关键帧");
        packet->m_body[0] = 0x17;
    }
    //类型
    packet->m_body[1] = 0x01;
    //时间戳
    packet->m_body[2] = 0x00;
    packet->m_body[3] = 0x00;
    packet->m_body[4] = 0x00;
    //数据长度 int 4个字节 相当于把int转成4个字节的byte数组
    packet->m_body[5] = (payload >> 24) & 0xff;
    packet->m_body[6] = (payload >> 16) & 0xff;
    packet->m_body[7] = (payload >> 8) & 0xff;
    packet->m_body[8] = (payload) & 0xff;

    //图片数据
    memcpy(&packet->m_body[9],p_payload,  payload);

    packet->m_hasAbsTimestamp = 0;
    packet->m_nBodySize = bodysize;
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nChannel = 0x10;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    callback(packet);
}

void VideoChannel::encodeData(int8_t *data) {
    LOGE("--->  encodeData ");
//    x264编码
    memcpy(pic_in->img.plane[0], data, ySize);
    for (int i = 0; i < uvSize; ++i) {
//u 1   v 2  //u数据
        *(pic_in->img.plane[1] + i) = *(data + ySize + i * 2 + 1);
        //v数据
        *(pic_in->img.plane[2] + i) = *(data + ySize + i * 2);
    }
    //编码出的数据 H264  一帧      nalu
    x264_nal_t *pp_nals;
    //编码出了几个 nalu （暂时理解为帧）  1   pi_nal  1  永远是1
    int pi_nal;
//编码出的参数  BufferInfo
    x264_picture_t pic_out;
    //编码出的数据 H264
    x264_encoder_encode(videoCodec, &pp_nals, &pi_nal, pic_in, &pic_out);
//借助java  --》文件
//    if (pi_nal > 0) {
//
//        for (int i = 0; i < pi_nal; ++i) {
//            LOGE("i  %d",i);
//            javaCallHelper->postH264(reinterpret_cast<char *>(pp_nals[i].p_payload), pp_nals[i].i_payload);
//        }
//    } sps  pps    x264  sps  pps
    uint8_t sps[100];
    uint8_t pps[100];
    int sps_len, pps_len;
    for (int i = 0; i < pi_nal; ++i) {
        if (pp_nals[i].i_type == NAL_SPS) {
            // 去掉 00 00 00 01
            sps_len = pp_nals[i].i_payload - 4;
            memcpy(sps, pp_nals[i].p_payload + 4, sps_len);
//            要1  不要2      要 1  不要 2
//按照  硬解码  sps ps   数组 I帧  不要
        } else if (pp_nals[i].i_type == NAL_PPS) {
            pps_len = pp_nals[i].i_payload - 4;
            memcpy(pps, pp_nals[i].p_payload + 4, pps_len);
            //拿到pps 就表示 sps已经拿到了
            sendSpsPps(sps, pps, sps_len, pps_len);

        } else {
            //关键帧、非关键帧
            sendFrame(pp_nals[i].i_type,pp_nals[i].i_payload,pp_nals[i].p_payload);
        }

    }




    LOGE("--->  encodeData 2");
}

void VideoChannel::setVideoCallback(VideoChannel::VideoCallback callback) {
    this->callback = callback;
}
