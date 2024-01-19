//
// Created by maniu on 2022/8/5.
//

#include <pthread.h>
#include "MNFFmpeg.h"
void *decodeFFmpeg(void *data)
{
    MNFFmpeg *mnFFmpeg = (MNFFmpeg *) data;
    mnFFmpeg->decodeFFmpegThread();
    pthread_exit(&mnFFmpeg->decodeThread);


}
void MNFFmpeg::parpared() {
    pthread_create(&decodeThread, NULL, decodeFFmpeg, this);
}


void MNFFmpeg::decodeFFmpegThread() {
//      初始化
    pthread_mutex_lock(&init_mutex);
    av_register_all();
    avformat_network_init();
    pFormatCtx = avformat_alloc_context();
    if(avformat_open_input(&pFormatCtx, url, NULL, NULL) != 0)
    {
        if(LOG_DEBUG)
        {
            LOGE("can not open url :%s", url);
        }
        return;
    }
    LOGE("------------->6");
    if(avformat_find_stream_info(pFormatCtx, NULL) < 0)
    {
        if(LOG_DEBUG)
        {
            LOGE("can not find streams from %s", url);
        }
        return;
    }

    for(int i = 0; i < pFormatCtx->nb_streams; i++) {
        if (pFormatCtx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO)//得到音频流
        {
            if(audio == NULL)
            {
                audio = new MNAudio(playstatus, pFormatCtx->streams[i]->codecpar->sample_rate,callJava);
                audio->streamIndex = i;
                audio->codecpar = pFormatCtx->streams[i]->codecpar;
                audio->time_base = pFormatCtx->streams[i]->time_base;

//                总时间
                audio->duration = pFormatCtx->duration/ AV_TIME_BASE;
                duration = audio->duration;
            }
        }else if(pFormatCtx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO)
        {
//            如果是视频
            if(video == NULL) {
                video = new MNVideo(playstatus, callJava);
                video->streamIndex = i;
                video->codecpar = pFormatCtx->streams[i]->codecpar;
            }
        }
    }
    if(audio!=NULL){
        getCodecContext(audio->codecpar, &audio->avCodecContext);
    }
    if(video != NULL)
    {
        getCodecContext(video->codecpar, &video->avCodecContext);
    }
    pthread_mutex_unlock(&init_mutex);

//    子线程   linux
    callJava->onCallParpared(CHILD_THREAD);

//    回调 java    播放器  要1    不要 2

}

int MNFFmpeg::getCodecContext(AVCodecParameters *codecpar, AVCodecContext **avCodecContext) {
    AVCodec *dec = avcodec_find_decoder(codecpar->codec_id);
    if(!dec)
    {
        exit=true;
        pthread_mutex_unlock(&init_mutex);
        return -1;
    }
    *avCodecContext = avcodec_alloc_context3(dec);
    if(!*avCodecContext)
    {
        exit = true;
        pthread_mutex_unlock(&init_mutex);
        return -1;
    }
    if(avcodec_parameters_to_context(*avCodecContext, codecpar) < 0)
    {
        exit = true;
        pthread_mutex_unlock(&init_mutex);
        return -1;
    }

    if(avcodec_open2(*avCodecContext, dec, 0) != 0)
    {
        exit = true;
        pthread_mutex_unlock(&init_mutex);
        return -1;
    }
    LOGE("------------->");
    return 0;
}

void MNFFmpeg::start() {
    if(audio == NULL)
    {
        if(LOG_DEBUG)
        {
            LOGE("audio is null");
            return;
        }
    }
    audio->play();
    video->play();
    video->audio = audio;
    int count = 0;
    while(playstatus != NULL && !playstatus->exit)
    {
        if(playstatus->seek)
        {
            continue;
        }
//        放入队列
        if(audio->queue->getQueueSize() > 40||video->queue->getQueueSize() > 40){
            continue;
        }
        AVPacket *avPacket = av_packet_alloc();

//        用户停止了       最后一个 文件
        if(av_read_frame(pFormatCtx, avPacket) == 0)
        {
            if(avPacket->stream_index == audio->streamIndex)
            {
                audio->queue->putAvpacket(avPacket);
            } else if(avPacket->stream_index == video->streamIndex)
            {
                LOGE("解码视频第 %d 帧", count);
                video->queue->putAvpacket(avPacket);
            }
            else{
                av_packet_free(&avPacket);
                av_free(avPacket);
            }
        } else {
            av_packet_free(&avPacket);
            av_free(avPacket);
//特殊情况
            while(playstatus != NULL && !playstatus->exit)
            {
                if(audio->queue->getQueueSize() > 0)
                {
                    continue;
                }

                if(video->queue->getQueueSize() > 0)
                {
                    continue;
                }
                playstatus->exit = true;
                break;
            }
        }

        if(playstatus != NULL && playstatus->exit)
        {
            audio->queue->clearAvpacket();
            playstatus->exit = true;

        }
    }

}
MNFFmpeg::MNFFmpeg(MNPlaystatus *playstatus,MNCallJava *callJava, const char *url) {
    this->callJava = callJava;
    this->url = url;
    this->playstatus = playstatus;
    pthread_mutex_init(&seek_mutex, NULL);
    pthread_mutex_init(&init_mutex, NULL);
}

void MNFFmpeg::pause() {
    if(audio != NULL)
    {
        audio->pause();
    }

}

void MNFFmpeg::seek(jint secds) {
    if (duration <= 0) {
        return;
    }

    if (secds >= 0 && secds <= duration) {
        pthread_mutex_lock(&seek_mutex);
        if (audio != NULL) {
            playstatus->seek = true;
            audio->queue->clearAvpacket();
            audio->clock = 0;
            audio->last_tiem = 0;

//            s    *  us
            int64_t rel = secds * AV_TIME_BASE;
            avformat_seek_file(pFormatCtx, -1, INT64_MIN, rel, INT64_MAX, 0);

            playstatus->seek = false;
        }
        pthread_mutex_unlock(&seek_mutex);
    }

}

void MNFFmpeg::resume() {
    if(audio != NULL)
    {
        audio->resume();
    }
}

void MNFFmpeg::setMute(jint mute) {
    if(audio != NULL)
    {
        audio->setMute(mute);
    }
}


