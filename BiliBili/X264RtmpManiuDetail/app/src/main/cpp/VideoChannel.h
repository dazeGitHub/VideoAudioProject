//
// Created by maniu on 2022/6/28.
//

#ifndef X264RTMPMANIU_VIDEOCHANNEL_H
#define X264RTMPMANIU_VIDEOCHANNEL_H
#include <jni.h>
#include <x264.h>

class VideoChannel {
public:
    VideoChannel();
    ~VideoChannel();
    //创建x264编码器
    void setVideoEncInfo(int width, int height, int fps, int bitrate);
//真正开始编码一帧数据
    void encodeData(int8_t *data);
private:
    x264_picture_t *pic_in = 0;
    x264_t *videoCodec = 0;
    int mWidth;
    int mHeight;
    int mFps;
    int mBitrate;
    int ySize;
    int uvSize;
};


#endif //X264RTMPMANIU_VIDEOCHANNEL_H
