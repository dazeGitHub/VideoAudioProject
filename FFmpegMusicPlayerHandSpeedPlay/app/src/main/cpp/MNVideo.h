//
// Created by maniu on 2021/4/18.
//

#ifndef FFMPEGMUSICPLAYER_MNVIDEO_H
#define FFMPEGMUSICPLAYER_MNVIDEO_H


#include "MNPlaystatus.h"
#include "MNCallJava.h"
#include "MNQueue.h"
#include "MNAudio.h"


extern "C"
{
#include "libavcodec/avcodec.h"
#include "libavutil/time.h"
#include <libavutil/imgutils.h>
#include <libswscale/swscale.h>
};

class MNVideo {
public:
    MNQueue *queue = NULL;
    int streamIndex = -1;
    AVCodecContext *avCodecContext = NULL;
    AVCodecParameters *codecpar = NULL;
    MNPlaystatus *playstatus = NULL;
    MNCallJava *wlCallJava = NULL;
    pthread_mutex_t codecMutex;
    pthread_t thread_play;
//    -------------------新加--------------
    double clock = 0;
//实时计算出来   主要与音频的差值
    double delayTime = 0;
//    默认休眠时间   40ms  0.04s    帧率 25帧
    double defaultDelayTime = 0.04;
    MNAudio *audio = NULL;
    AVRational time_base;
public:
    MNVideo(MNPlaystatus *playstatus, MNCallJava *wlCallJava);
    ~MNVideo();
    void play();
    double getDelayTime(double diff);
    double getFrameDiffTime(AVFrame *avFrame);

    void pause();
};


#endif //FFMPEGMUSICPLAYER_MNVIDEO_H
