//
// Created by maniu on 2022/8/12.
//

#ifndef WANGYIMUSICPLAYER_MNVIDEO_H
#define WANGYIMUSICPLAYER_MNVIDEO_H


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
    MNAudio *audio = NULL;
    //    默认休眠时间   40ms  0.04s    帧率 25帧
    double defaultDelayTime = 0.04;
    double clock = 0;
//实时计算出来   主要与音频的差值  休眠
    double delayTime = 0;
    AVRational time_base;
public:
    MNVideo(MNPlaystatus *playstatus, MNCallJava *wlCallJava);
    ~MNVideo();
    void play();
    void pause();
    double getDelayTime(double diff);
//    算出与音频相差的时间，  得到最合适的时间    调整    快 慢
    double getFrameDiffTime(AVFrame *avFrame);
    void resume();
};

#endif //WANGYIMUSICPLAYER_MNVIDEO_H
