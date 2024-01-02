//
// Created by maniu on 2022/6/28.
//

#ifndef X264RTMPMANIU_VIDEOCHANNEL_H
#define X264RTMPMANIU_VIDEOCHANNEL_H
#include <jni.h>
#include <x264.h>
#include "JavaCallHelper.h"
#include "librtmp/rtmp.h"
class VideoChannel {
    typedef void (*VideoCallback)(RTMPPacket* packet);
public:
    VideoChannel();
    ~VideoChannel();
    //创建x264编码器
    void setVideoEncInfo(int width, int height, int fps, int bitrate);
//真正开始编码一帧数据
    void encodeData(int8_t *data);
    void setVideoCallback(VideoCallback callback);
    void sendSpsPps(uint8_t *sps, uint8_t *pps, int len, int pps_len);
    void sendFrame(int type, int payload, uint8_t *p_payload);
private:
    x264_picture_t *pic_in = 0;
    x264_t *videoCodec = 0;
    int mWidth;
    int mHeight;
    int mFps;
    int mBitrate;
    int ySize;
    int uvSize;
    VideoCallback callback;
public:
    JavaCallHelper *javaCallHelper;
};


#endif //X264RTMPMANIU_VIDEOCHANNEL_H
