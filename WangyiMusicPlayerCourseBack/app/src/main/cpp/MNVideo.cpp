//
// Created by maniu on 2022/8/12.
//

#include <libavutil/pixfmt.h>
#include "MNVideo.h"
#include "AndroidLog.h"

MNVideo::MNVideo(MNPlaystatus *playstatus, MNCallJava *wlCallJava) {
    this->playstatus = playstatus;
    this->wlCallJava = wlCallJava;
    queue = new MNQueue(playstatus);
    pthread_mutex_init(&codecMutex, NULL);
}

void *playVideo(void *data) {
    MNVideo *video = static_cast<MNVideo *>(data);

    while (video->playstatus != NULL && !video->playstatus->exit) {
        if (video->playstatus->seek) {
            av_usleep(1000 * 100);
            continue;
        }
//        if (video->playstatus->pause) {
//            av_usleep(1000 * 100);
//            continue;
//        }

        if (video->queue->getQueueSize() == 0) {

            if (!video->playstatus->load) {
//                努力加载中
                video->playstatus->load = true;
                video->wlCallJava->onCallLoad(CHILD_THREAD, true);
                av_usleep(1000 * 100);
                continue;
            }

        }
        pthread_mutex_lock(&video->codecMutex);

        AVPacket *avPacket = av_packet_alloc();

        if (video->queue->getAvpacket(avPacket) != 0) {
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            continue;
        }
//
        AVFrame *avFrame = av_frame_alloc();
        //解码操作
        if(avcodec_send_packet(video->avCodecContext, avPacket) != 0)
        {
//            括号就失败了
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            pthread_mutex_unlock(&video->codecMutex);
            continue;
        }
        if (avcodec_receive_frame(video->avCodecContext, avFrame) != 0) {
            av_frame_free(&avFrame);
            av_free(avFrame);
            avFrame = NULL;
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            pthread_mutex_unlock(&video->codecMutex);
            continue;//凡是contline  break  return  pthread_mutex_unlock
        }
        pthread_mutex_unlock(&video->codecMutex);

//        avFrame  -------RGB  surfaceview  需要 1 不需要2   opengl   摄像头  yuv  屏幕

//opengsl

        if (avFrame->format == AV_PIX_FMT_YUV420P) {
//            直接回调
//            LOGE("当前视频是YUV420P格式");
//            avFrame->data[0]//y
//            avFrame->data[1];//u
//            avFrame->data[2];//v  ffmpeg   我给你 用    扩容    0  1  2   camerx
//
//        算出 时间
//diff  大于 0   说明音频快       视频速度  跟上，，   视频休眠时间 要减小

//diff  小于 0   说明视频频快       视频速度慢一些，，   视频休眠时间 要增大
            double diff = video->getFrameDiffTime(avFrame);
//默认休眠 休眠时间 defaultDelayTime  上下浮动
            double delay = video->getDelayTime(diff);
//算出时间
            av_usleep(video->defaultDelayTime);
            video->wlCallJava->onCallRenderYUV(video->avCodecContext->width,
                                               video->avCodecContext->height,
                                               avFrame->data[0],
                                               avFrame->data[1],
                                               avFrame->data[2]);
            LOGE("当前视频是YUV420P格式");
        } else {
            LOGE("当前视频不是YUV420P格式");
            AVFrame *pFrameYUV420P = av_frame_alloc();
            int num = av_image_get_buffer_size(
                    AV_PIX_FMT_YUV420P,
                    video->avCodecContext->width,
                    video->avCodecContext->height,
                    1);

            uint8_t *buffer = static_cast<uint8_t *>(av_malloc(num * sizeof(uint8_t)));
//            ffmpeg   得我们自己做
            av_image_fill_arrays(
                    pFrameYUV420P->data,
                    pFrameYUV420P->linesize,
                    buffer,
                    AV_PIX_FMT_YUV420P,
                    video->avCodecContext->width,
                    video->avCodecContext->height,
                    1);
            SwsContext *sws_ctx = sws_getContext(
                    video->avCodecContext->width,
                    video->avCodecContext->height,
                    video->avCodecContext->pix_fmt,
                    video->avCodecContext->width,
                    video->avCodecContext->height,
                    AV_PIX_FMT_YUV420P,
                    SWS_BICUBIC, NULL, NULL, NULL);

            if (!sws_ctx) {
                av_frame_free(&pFrameYUV420P);
                av_free(pFrameYUV420P);
                av_free(buffer);
                pthread_mutex_unlock(&video->codecMutex);
                continue;
            }
            sws_scale(
                    sws_ctx,
                    reinterpret_cast<const uint8_t *const *>(avFrame->data),
                    avFrame->linesize,
                    0,
                    avFrame->height,
                    pFrameYUV420P->data,
                    pFrameYUV420P->linesize);
//            pFrameYUV420P->data   =buffer
            video->wlCallJava->onCallRenderYUV(video->avCodecContext->width,
                                               video->avCodecContext->height,
                                               pFrameYUV420P->data[0],
                                               pFrameYUV420P->data[1],
                                               pFrameYUV420P->data[2]);

            av_frame_free(&pFrameYUV420P);
            av_free(pFrameYUV420P);
            av_free(buffer);
            sws_freeContext(sws_ctx);
        }
//avFrame->data[0]   width*heigth    u

        av_frame_free(&avFrame);
        av_free(avFrame);
        avFrame = NULL;
        av_packet_free(&avPacket);
        av_free(avPacket);
        avPacket = NULL;
        pthread_mutex_unlock(&video->codecMutex);

    }
    pthread_exit(&video->thread_play);

}

void MNVideo::play() {
    //    子线程播放   解码
    pthread_create(&thread_play, NULL, playVideo, this);

}

void MNVideo::pause() {
    queue->lock();
}

void MNVideo::resume() {
    queue->unlock();
}
//和音频的时间差
double MNVideo::getFrameDiffTime(AVFrame *avFrame) {
//
    double pts = av_frame_get_best_effort_timestamp(avFrame);
    if(pts == AV_NOPTS_VALUE)
    {
        pts = 0;
    }
//    pts  时间    时间单位 时间基
//    pts *=time_base.num / (double)time_base.den
//等价
    pts *= av_q2d(time_base);
    if(pts > 0)
    {
        clock = pts;
    }
//  diff  大于 0   说明音频快, 那么 视频速度 要跟上, 所以视频休眠时间 要减小
//  diff  小于 0   说明视频快, 那么 视频速度 要慢一些, 所以视频休眠时间 要增大
    double diff = audio->clock - clock;
    return diff;
}
//diff  秒
double MNVideo::getDelayTime(double diff) {

//diff  大于 0   说明音频快       视频速度  跟上，，   视频休眠时间 要减小
//diff  小于 0   说明视频频快       视频速度慢一些，，   视频休眠时间 要增大

//    以3ms为界限  10ms 3ms   4ms
    if(diff > 0.003) {
//视频休眠时间 要减小
        delayTime = delayTime * 2 / 3;//
//如果delayTime  太小  20ms    小于 delayTime 10ms
        if(delayTime < defaultDelayTime / 2) {
//  40* 2/3
            delayTime = defaultDelayTime / 2;
//80  90ms
        } else if(delayTime > defaultDelayTime * 2){
            delayTime = defaultDelayTime * 2;
        }
    } else if(diff < - 0.003){
//        视频慢
        delayTime = delayTime * 3 / 2;//

        if(delayTime < defaultDelayTime / 2) {
//  40* 2/3
            delayTime = defaultDelayTime / 2;
//80  90ms
        } else if(delayTime > defaultDelayTime * 2){
            delayTime = defaultDelayTime * 2;
        }
    }
//  超过 5 秒直接清空队列, 从文件中读取后放入队列, 那么必定音视频同步
    if (diff >= 5 || diff<=-5) {
        queue->clearAvpacket();
        audio->queue->clearAvpacket();
        delayTime = defaultDelayTime;
    }
//
    return delayTime;
}

