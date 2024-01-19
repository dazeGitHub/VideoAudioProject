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
    MNAudio *audio = NULL; //因为视频和音频同步以 音频为准, 所以 MNVideo 里边必须持有 MNAudio
public:
    MNVideo(MNPlaystatus *playstatus, MNCallJava *wlCallJava);
    ~MNVideo();
    void play();
    void pause();
};

#endif //WANGYIMUSICPLAYER_MNVIDEO_H
