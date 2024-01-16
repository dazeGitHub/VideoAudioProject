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

class MNFFmpeg {
public:
    const char* url = NULL;
    pthread_t decodeThread;
    AVFormatContext *pFormatCtx = NULL;
    MNAudio *audio = NULL;
    MNCallJava *callJava = NULL;
    MNPlaystatus *playstatus = NULL;
    int duration = 0;
    pthread_mutex_t seek_mutex;
public:
    MNFFmpeg(MNPlaystatus *playstatus,MNCallJava *callJava, const char *url);
    void parpared();
    void decodeFFmpegThread();
    void start();
    void pause();

    void seek(jint i);

    void resume();

    void setMute(jint i);
};


#endif //WANGYIMUSICPLAYER_MNFFMPEG_H
