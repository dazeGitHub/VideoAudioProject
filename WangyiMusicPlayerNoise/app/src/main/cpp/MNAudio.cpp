//
// Created by maniu on 2022/8/5.
//

#include <pthread.h>

#include "MNAudio.h"
#include "AndroidLog.h"
MNAudio::MNAudio(MNPlaystatus *playstatus, int sample_rate, MNCallJava *callJava) {
    this->playstatus = playstatus;
    this->sample_rate = sample_rate;
//     采样频率  通道数    采样位数
    buffer = (uint8_t *) av_malloc(sample_rate * 2 * 2);
    queue = new MNQueue(playstatus);
    this->callJava = callJava;
    soundTouch = new SoundTouch();
    sampleBuffer= static_cast<SAMPLETYPE *>(malloc(sample_rate * 2 * 2));
//   设值采样频率
    soundTouch->setSampleRate(sample_rate);
//    设值通道数据
    soundTouch->setChannels(2);
    soundTouch->setTempo(1.5); //速度调整为 1.5 倍
    float pitch = 2.0f;
    soundTouch->setPitch(
            pitch
    );//变调
}
//c
void *decodPlay(void *data) {
    MNAudio *mnAudio = (MNAudio *) data;
    mnAudio->initOpenSLES();
    pthread_exit(&mnAudio->thread_play);

}

//C++

void MNAudio::play() {
    pthread_create(&thread_play, NULL, decodPlay, this);

}
//播放   喇叭需要取出数据   ---》播放  主动了   孩子    肚子饿    不断吃
//  喇叭  --- pcmBufferCallBack 主动调用
void pcmBufferCallBack(SLAndroidSimpleBufferQueueItf bf, void * context)
{
//    audio  解码    送到AudioTrack 被动   口中
//         opengsl  es  主动    他来进行出发  解码
    MNAudio *mnAudio = (MNAudio *) context;
    if(mnAudio != NULL) {
//        提供pcm数据 实际大小    喇叭配置  44100   2   2            数据量  44100*2 *2 个字节    播放 需要花多少
// 1s
        int buffersize = mnAudio->getSoundTouchData();//mnAudio->resampleAudio()
        if(buffersize > 0)
        {
//             mnAudio->clock  永远大于   帧 携带时间    配置 数据   播放 不肯能

// 一天 一顿   一天   5年   音频倍数
            mnAudio->clock+=buffersize/((double)(mnAudio->sample_rate * 2 * 2));
            if(mnAudio->clock - mnAudio->last_tiem >= 0.1){
                mnAudio->last_tiem = mnAudio->clock;
                mnAudio->callJava->onCallTimeInfo(CHILD_THREAD,mnAudio->clock,mnAudio->duration);
            }
//            往喇叭 方整理后 波形 facc
            (*mnAudio->pcmBufferQueue)->Enqueue(mnAudio->pcmBufferQueue, (char *) mnAudio-> sampleBuffer,
                                                buffersize);
//音频的数据送
        }
    }

}
void MNAudio::initOpenSLES() {
//    创建引擎 engineObject  创建  操作 直接     拿到对应节  接口
    SLresult result;
    result= slCreateEngine(&engineObject, 0, 0,
                            0, 0, 0);
    if(result!=SL_RESULT_SUCCESS) {
        return;
    }

    LOGE("-------->initOpenSLES  1 ");
//初始化 引擎  false 同步  true 异步
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);

    if(result!=SL_RESULT_SUCCESS) {
        return;
    }
//    opensl es   对象      创建它 CreateOutputMix    初始化  Realize   调用    拿到接口  GetInterface
//    engineEngine   引擎接口
//创建混音器
    const SLInterfaceID mids[1] = {SL_IID_ENVIRONMENTALREVERB};
    const SLboolean mreq[1] = {SL_BOOLEAN_FALSE};
    result =(*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 1,mids, mreq);
    if(result!=SL_RESULT_SUCCESS) {
        return;
    }
    LOGE("-------->initOpenSLES  2");
//    初始化
    result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    if(result!=SL_RESULT_SUCCESS) {
        return;
    }
//    拿到接口

    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_ENVIRONMENTALREVERB, &outputMixEnvironmentalReverb);
    if(result!=SL_RESULT_SUCCESS) {
        return;
    }
    SLDataFormat_PCM pcm={
            SL_DATAFORMAT_PCM,//播放pcm格式的数据
            2,//2个声道（立体声）
            static_cast<SLuint32>(getCurrentSampleRateForOpensles(sample_rate)),//44100hz的频率
            SL_PCMSAMPLEFORMAT_FIXED_16,//位数 16位
            SL_PCMSAMPLEFORMAT_FIXED_16,//和位数一致就行
            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,//立体声（前左前右）
            SL_BYTEORDER_LITTLEENDIAN//结束标志
    };
    LOGE("-------->initOpenSLES 3 ");
//    流数据
    SLDataLocator_AndroidSimpleBufferQueue android_queue={SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,2};

//    配置混音器
    SLDataLocator_OutputMix outputMix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};
    SLDataSink audioSnk ={&outputMix, 0};
//   播放器跟混音器建立联系

    LOGE("-------->initOpenSLES 4 ");
    SLDataSource slDataSource = {&android_queue, &pcm};
//SLDataSource *pAudioSrc,  音频源  音频配置
    const SLInterfaceID ids[3] = {SL_IID_BUFFERQUEUE,SL_IID_VOLUME,SL_IID_MUTESOLO};
    const SLboolean req[3] = {SL_BOOLEAN_TRUE,SL_BOOLEAN_TRUE,SL_BOOLEAN_TRUE};
//传建播放器对象
    (*engineEngine)->CreateAudioPlayer(engineEngine,&pcmPlayerObject,&slDataSource, &audioSnk,
    2,ids ,req);
    //初始化播放器
    (*pcmPlayerObject)->Realize(pcmPlayerObject, SL_BOOLEAN_FALSE);
//    得到接口后调用  获取Player接口 //    得到接口后调用  获取Player接口
    (*pcmPlayerObject)->GetInterface(pcmPlayerObject, SL_IID_PLAY, &pcmPlayerPlay);
//是否静音接口
    (*pcmPlayerObject)->GetInterface(pcmPlayerObject, SL_IID_MUTESOLO, &pcmMutePlay);

    //   拿控制  播放暂停恢复的句柄
    (*pcmPlayerObject)->GetInterface(pcmPlayerObject,SL_IID_VOLUME,&pcmVolumePlay);
//    注册回调缓冲区 获取缓冲队列接口
    (*pcmPlayerObject)->GetInterface(pcmPlayerObject, SL_IID_BUFFERQUEUE, &pcmBufferQueue);

//喇叭  你该怎么拉数据
    (*pcmBufferQueue)->RegisterCallback(pcmBufferQueue, pcmBufferCallBack, this);
//    操作播放器的接口 播放器 也不能 播放器播放

//    获取播放状态接口
        (*pcmPlayerPlay)->SetPlayState(pcmPlayerPlay, SL_PLAYSTATE_PLAYING);
    LOGE("-------->initOpenSLES 5 ");
//    播放接口
//声道接口
//暂停恢复 接口
//注册 缓冲接口
//注册回调缓冲接口  激活
    pcmBufferCallBack(pcmBufferQueue, this);
    LOGE("-------->initOpenSLES 6");
}

int MNAudio::getCurrentSampleRateForOpensles(int sample_rate) {
    int rate = 0;
    switch (sample_rate)
    {
        case 8000:
            rate = SL_SAMPLINGRATE_8;
            break;
        case 11025:
            rate = SL_SAMPLINGRATE_11_025;
            break;
        case 12000:
            rate = SL_SAMPLINGRATE_12;
            break;
        case 16000:
            rate = SL_SAMPLINGRATE_16;
            break;
        case 22050:
            rate = SL_SAMPLINGRATE_22_05;
            break;
        case 24000:
            rate = SL_SAMPLINGRATE_24;
            break;
        case 32000:
            rate = SL_SAMPLINGRATE_32;
            break;
        case 44100:
            rate = SL_SAMPLINGRATE_44_1;
            break;
        case 48000:
            rate = SL_SAMPLINGRATE_48;
            break;
        case 64000:
            rate = SL_SAMPLINGRATE_64;
            break;
        case 88200:
            rate = SL_SAMPLINGRATE_88_2;
            break;
        case 96000:
            rate = SL_SAMPLINGRATE_96;
            break;
        case 192000:
            rate = SL_SAMPLINGRATE_192;
            break;
        default:
            rate =  SL_SAMPLINGRATE_44_1;

    }
    return rate;

}


//解码一帧数据
int MNAudio::resampleAudio(void **pcmbuf) {

    while(playstatus != NULL && !playstatus->exit)
    {
        avPacket = av_packet_alloc();
        if(queue->getAvpacket(avPacket) != 0)
        {
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            continue;
        }
        ret = avcodec_send_packet(avCodecContext, avPacket);
        if(ret != 0)
        {
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            continue;
        }
        avFrame = av_frame_alloc();
//        解码核心函数
        ret = avcodec_receive_frame(avCodecContext, avFrame);
        if(ret == 0) {
            SwrContext *swr_ctx;
// 设值输入参数 和 输出参数
            swr_ctx = swr_alloc_set_opts(
                    NULL,
                    AV_CH_LAYOUT_STEREO,
                    AV_SAMPLE_FMT_S16,
                    avFrame->sample_rate,
                    avFrame->channel_layout,
                    (AVSampleFormat) avFrame->format,
                    avFrame->sample_rate,
                    NULL, NULL
            );
            if(!swr_ctx || swr_init(swr_ctx) <0)
            {
//                avPacket  ---data  容器 malloc
                av_packet_free(&avPacket);
//                avPacket
                av_free(avPacket);
                avPacket = NULL;

                av_frame_free(&avFrame);
                av_free(avFrame);
                avFrame = NULL;

                swr_free(&swr_ctx);
                continue;
            }

//            开始转换   swr_convert
            nb = swr_convert(
                    swr_ctx,
                    &buffer,
                    avFrame->nb_samples,
                    (const uint8_t **) avFrame->data,
                    avFrame->nb_samples);
            int out_channels = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);
//获取转换后的真实大小    audiotrack        过滤
            data_size = nb * out_channels * av_get_bytes_per_sample(AV_SAMPLE_FMT_S16);
            *pcmbuf = buffer;
//            avFrame 等不等于  播放时 等于 1 不等于 2
//时间戳  单位时间  解码时间  =    244ms       100ms    344ms
//            now_time= avFrame->pts * (time_base.num / (double)  time_base.den);
            now_time= avFrame->pts * av_q2d(time_base);
            if(now_time < clock)
            {
                now_time = clock;
            }
            clock = now_time;
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            av_frame_free(&avFrame);
            av_free(avFrame);
            avFrame = NULL;
            swr_free(&swr_ctx);
            break;

        } else{
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;

            av_frame_free(&avFrame);
            av_free(avFrame);
            avFrame = NULL;
            continue;
        }
    }
    return data_size;
}

void MNAudio::pause() {
    if(pcmPlayerPlay != NULL)
    {
        (*pcmPlayerPlay)->SetPlayState(pcmPlayerPlay, SL_PLAYSTATE_PAUSED);
    }
}

void MNAudio::resume() {
    if(pcmPlayerPlay != NULL)
    {

        (*pcmPlayerPlay)->SetPlayState(pcmPlayerPlay, SL_PLAYSTATE_PLAYING);
    }
}
void MNAudio::setVolume(int percent) {

    if(pcmVolumePlay != NULL) {
        if (percent > 30) {
            (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -20);
        } else if (percent > 25) {
            (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -22);
        } else if (percent > 20) {
            (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -25);
        } else if (percent > 15) {
            (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -28);
        } else if (percent > 10) {
            (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -30);
        } else if (percent > 5) {
            (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -34);
        } else if (percent > 3) {
            (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -37);
        } else if (percent > 0) {
            (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -40);
        } else {
            (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -100);
        }


    }

}
void MNAudio::setMute(int mute) {
    LOGE(" 声道  接口%p", pcmMutePlay);
    LOGE(" 声道  接口%d", mute);
    if(pcmMutePlay == NULL)
    {
        return;
    }
    this->mute = mute;
    if(mute == 0)//right   0   做通道播放
    {
        (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 1, false);
        (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 0, true);

    } else if(mute == 1)//left
    {
        (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 1, true);
        (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 0, false);
    }else if(mute == 2)//center
    {

        (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 1, false);
        (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 0, false);
    }
}
////获得整理之后的波形  波形 数 变小 1  标多  2
//int MNAudio::getSoundTouchData() {
//    while(playstatus != NULL && !playstatus->exit){
//        out_buffer = NULL;
//        if(finished){
////           开始整理波形  没有完成
//            finished = false;
////            需要得到数据
//            data_size  = this->resampleAudio(reinterpret_cast<void **>(&out_buffer));
////data_size  2M  10M 音频恒定 data_size  101      data_size / 2  50     51
//            if (data_size > 0) {
//                     for(int i = 0; i < data_size / 2 ; i++){
////                    完整的音频值  有 1  没有2   偶数
//                         sampleBuffer[i] = ( ((out_buffer[i * 2 + 1]) << 8) | out_buffer[i * 2] );
//
//                     }
////                     for循环完毕后 整理模型 nb 数据放进去
////avcodec_send_packet  把原始波形数据 放进去   sampleBuffer（值换 ）
//                 soundTouch->putSamples(sampleBuffer, nb);
////                    0  d代表还可以放     非  0 代表具体      波形多大
//                num= soundTouch->receiveSamples(sampleBuffer, data_size/4);
//                LOGE("------------第一个num %d ",num);
//            }
//            else{
//                soundTouch->flush();
//            }
//
//
//        }
//        if (num == 0) {
//            finished = true;
//            continue;
//        } else{
////            num 是实际值
//            if(out_buffer == NULL) {
//                num = soundTouch->receiveSamples(sampleBuffer, data_size / 4);
//                LOGE("------------第二个num %d ",num);
//
//                if(num == 0)
//                {
//                    finished = true;
//                    continue;
//                }
//
//            }
//            LOGE("---------------- 结束1 -----------------------d%",num)
//            return num;
//            }
//
//
//
//        }
//
//    return 0;
//
//    }

int MNAudio::getSoundTouchData() {
//        重新整理波形  sountouch
//我们先取数据   pcm 的数据  就在  outbuffer
    while(playstatus != NULL && !playstatus->exit){
        LOGE("------------------循环---------------------------finished %d",finished)
        out_buffer = NULL;
        if(finished){
//          开始整理波形, 没有完成
            finished = false;
//            从网络流 文件 读取数据 out_buffer  字节数量  out_buffer   是一个旧波
            data_size = this->resampleAudio(reinterpret_cast<void **>(&out_buffer));
            if (data_size > 0) { //有波形
//              遍历解码后的每个 pcm 的帧, 音频都比较小
                for(int i = 0; i < data_size / 2 + 1; i++){
//                  short  2个字节  pcm数据   ====波形
//                  前面是低8位(偶数索引), 后面是高8位(奇数索引), sampleBuffer 就是完整波形了
                    sampleBuffer[i] = (out_buffer[i * 2] | ((out_buffer[i * 2 + 1]) << 8));
                }
//              丢给sountouch   进行波的整理
//              把原始波形数据 放进去
                soundTouch->putSamples(sampleBuffer, nb);
//              接受一个新波 sampleBuffer, 最大接收数据量是 data_size / 4, num 是 0 表示还可以放
//              如果到了 data_size / 4 那么结束循环
                num = soundTouch->receiveSamples(sampleBuffer, data_size / 4);
                LOGE("------------第一个num %d ",num);
            }else{
                soundTouch->flush();
            }
        }

        if (num == 0) {
            finished = true;
            continue;
        } else{
            if(out_buffer == NULL){
//              num 是实际值, 还要接收一次
                num = soundTouch->receiveSamples(sampleBuffer, data_size / 4);
                LOGE("------------第二个num %d ",num);
                if(num == 0)
                {
                    finished = true;
                    continue;
                }
            }
            LOGE("---------------- 结束1 -----------------------")
            return num;
        }

    }
    LOGE("---------------- 结束2 -----------------------")
    return 0;
}
