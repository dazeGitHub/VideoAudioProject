//
// Created by maniu on 2022/8/5.
//

#ifndef WANGYIMUSICPLAYER_MNFFMPEG_H
#define WANGYIMUSICPLAYER_MNFFMPEG_H

extern "C"
{
#include <libavformat/avformat.h>
};

#include "MNAudio.h"
#include "MNCallJava.h"
#include "MNPlaystatus.h"
#include "MNVideo.h"

class MNFFmpeg {
public:
    const char* url = NULL;
    pthread_t decodeThread;
    AVFormatContext *pFormatCtx = NULL;
    MNAudio *audio = NULL;
//    音频怎么写  视频怎么写
    MNVideo *video = NULL;

    MNCallJava *callJava = NULL;
    MNPlaystatus *playstatus = NULL;
    int duration = 0;
    pthread_mutex_t seek_mutex;
//初始化同步
    pthread_mutex_t init_mutex;

    bool exit = false;

public:
    MNFFmpeg(MNPlaystatus *playstatus,MNCallJava *callJava, const char *url);
    void parpared();
    void decodeFFmpegThread();
    void start();
    void pause();
    int getCodecContext(AVCodecParameters *codecpar, AVCodecContext **avCodecContext);

    void seek(jint i);

    void resume();

    void setMute(jint i);
};


#endif //WANGYIMUSICPLAYER_MNFFMPEG_H
