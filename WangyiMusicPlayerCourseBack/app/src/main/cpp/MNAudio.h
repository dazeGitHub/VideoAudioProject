//
// Created by maniu on 2022/8/5.
//

#ifndef WANGYIMUSICPLAYER_MNAUDIO_H
#define WANGYIMUSICPLAYER_MNAUDIO_H

#include "MNQueue.h"
#include "MNCallJava.h"
#include "SoundTouch.h"
extern "C"
{
    #include <libswresample/swresample.h>
    #include <SLES/OpenSLES.h>
    #include <SLES/OpenSLES_Android.h>
    #include <libavcodec/codec_par.h>
    #include <libavcodec/avcodec.h>
};
using namespace soundtouch;

class MNAudio {
public:
    AVCodecParameters *codecpar = NULL;
    AVCodecContext *avCodecContext = NULL;
    pthread_t thread_play;
    // 引擎对象
    SLObjectItf engineObject = NULL;
    // 引擎接口
    SLEngineItf engineEngine = NULL;
    //混音器对象
    SLObjectItf outputMixObject = NULL;
//混音器接口
    SLEnvironmentalReverbItf outputMixEnvironmentalReverb = NULL;
//播放器对象
    SLObjectItf pcmPlayerObject = NULL;
    //播放器操作接口
    SLPlayItf pcmPlayerPlay = NULL;
    int sample_rate = 0;
//静音接口
    SLMuteSoloItf  pcmMutePlay = NULL;
    SLVolumeItf pcmVolumePlay = NULL;
    MNCallJava *callJava = NULL;
    //缓冲器队列接口
    SLAndroidSimpleBufferQueueItf pcmBufferQueue = NULL;
    MNQueue *queue = NULL;
    MNPlaystatus *playstatus = NULL;
    int streamIndex = -1;
    double last_tiem; //上一次调用时间
    AVPacket *avPacket = NULL;
    AVFrame *avFrame = NULL;
    int ret = 0;
    uint8_t *buffer = NULL;
    int data_size = 0;
    int duration = 0;
    //当前时间
    double now_time;//当前frame时间

    double clock;//当前播放的时间    准确时间
//1ms  1ms
// 3ms   个  1   3m
//时间单位         总时间/帧数     单位时间     *   时间戳= pts  * 总时间/帧数
    AVRational time_base;
    jmethodID jmid_timeinfo;

    //立体声
    int mute = 2;
//    波处理完了没
    bool finished = true;
    uint8_t *out_buffer = NULL;
    int nb;
//整理之后的波形  大小
    int num = 0;
    SAMPLETYPE  *sampleBuffer = NULL;
    SoundTouch *soundTouch = NULL;

    float speed = 1.0f;
public:
    void play();
//    解码函数
    int resampleAudio(void **pcmbuf);
    void initOpenSLES();
    MNAudio(MNPlaystatus *playstatus, int sample_rate,MNCallJava *callJava);
    int getCurrentSampleRateForOpensles(int sample_rate);

    void pause();

    void resume();

    int getSoundTouchData();

    void setVolume(int percent);

    void setMute(int mute);

    void setSpeed(float speed);

    void setHigth(float higth);
};


#endif //WANGYIMUSICPLAYER_MNAUDIO_H
