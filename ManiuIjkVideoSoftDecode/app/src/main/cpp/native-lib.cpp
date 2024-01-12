
#include <jni.h>
#include <string>
#include <android/log.h>
#include <pthread.h>
#include <unistd.h>
#include "MNQueue.h"
#include <android/native_window_jni.h>
extern "C"{
#include <libavformat/avformat.h>
#include "libavcodec/avcodec.h"
#include <libswscale/swscale.h>
#include <libavutil/imgutils.h>
}
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,"初始化层",__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_INFO,"h264层",__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_INFO,"解码层",__VA_ARGS__)
#define LOGV(...) __android_log_print(ANDROID_LOG_INFO,"同步层",__VA_ARGS__)
#define LOGQ(...) __android_log_print(ANDROID_LOG_INFO,"队列层",__VA_ARGS__)
#define LOGA(...) __android_log_print(ANDROID_LOG_INFO,"音频",__VA_ARGS__)
//视频索引
int videoIndex = -1;
//音频索引
int audioIndex = -1;
//视频队列
MNQueue *videoQueue;
//音频队列
MNQueue *audioQueue;

//视频队列
uint8_t *outbuffer;
//ffmpeg  c 代码
ANativeWindow *nativeWindow;
AVCodecContext *videoContext;
AVFormatContext *avFormatContext;
ANativeWindow_Buffer windowBuffer;
AVFrame *rgbFrame;
int width;
int height;
bool isStart = false;
SwsContext *swsContext;
void *decodeVideo(void *pVoid) {
    LOGI("==========解码线程");
    while (isStart) {
        AVPacket *videoPacket = av_packet_alloc();
//        有数据    就会取出来  没有数据   阻塞
        videoQueue->get(videoPacket);
        int ret =avcodec_send_packet(videoContext, videoPacket);
        if (ret != 0) {
            av_packet_free(&videoPacket);
            av_free(videoPacket);
            videoPacket = NULL;
            continue;
        }
//        容器 饭缸
        AVFrame *videoFrame = av_frame_alloc();
//        解码   dsp芯片  都是  api    dsp    cpu 解码  宽高  显示格式 容器  data  是系统帮你做了

        avcodec_receive_frame(videoContext, videoFrame);
        videoFrame->data;
        if (ret != 0) {
            av_frame_free(&videoFrame);
            av_free(videoFrame);
            videoFrame = NULL;
            av_packet_free(&videoPacket);
            av_free(videoPacket);
            videoPacket = NULL;
            LOGE("=================");
            continue;
        }
//转换了
        sws_scale(swsContext, videoFrame->data, videoFrame->linesize, 0, videoContext->height,
                  rgbFrame->data, rgbFrame->linesize
        );
//        现在  1/5   基本  surface   导致     4/5
//入参 出参对象
//outBuffer
        ANativeWindow_lock(nativeWindow, &windowBuffer, NULL);
//目的地
        uint8_t *dstWindow = static_cast<uint8_t *>(windowBuffer.bits);
//       数据源
//    outbuffer
// 不可以  outbuffe
//        memcpy(dstWindow, outbuffer, width * height * 4);
        for (int i = 0; i < height; ++i) {
//
            memcpy(dstWindow+i*windowBuffer.stride*4, outbuffer+i * rgbFrame->linesize[0], rgbFrame->linesize[0]);

        }
        ANativeWindow_unlockAndPost(nativeWindow);
        av_frame_free(&videoFrame);
        av_free(videoFrame);
        videoFrame = NULL;
        av_packet_free(&videoPacket);
        av_free(videoPacket);
        videoPacket = NULL;
//        windowBuffer.bits;
//        解码
//avdecodeframe  周五再讲
//        avcodec_send_packet()
//        avcodec_receive_frame()
    }

}
void *decodePacket(void *pVoid) {
//子线程中
    LOGI("==========读取线程");

    while (isStart) {
        if (videoQueue->size() > 100) {
            usleep(100 * 1000);
        }
        AVPacket *avPacket = av_packet_alloc();
        int ret = av_read_frame(avFormatContext, avPacket);//压缩数据
        if (ret < 0) {
//            文件末尾
            break;
        }
        if (avPacket->stream_index == videoIndex) {
//视频包
            LOGD("视频包 %d", avPacket->size);
            videoQueue->push(avPacket);
        }else  if(avPacket->stream_index == audioIndex) {
//视频包


        }
    }

    return NULL;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_maniu_maniuijk_MNPlayer_play(JNIEnv *env, jobject instance, jstring url_,
                                      jobject surface) {

    jclass david_player = env->GetObjectClass(instance);
    jmethodID onSizeChange =env->GetMethodID(david_player, "onSizeChange", "(II)V");


    const char *url = env->GetStringUTFChars(url_, 0);
//    初始化ffmpeg的网络模块
    avformat_network_init();
//   初始化总上下文
    avFormatContext= avformat_alloc_context();
//    打开视频文件 C  对象  调用
    avformat_open_input(&avFormatContext, url, NULL, NULL);

    int code=avformat_find_stream_info(avFormatContext, NULL);
    if (code < 0) {
        env->ReleaseStringUTFChars(url_, url);
        return;
    }

    avFormatContext->nb_streams;
//    遍历流的个数   音频流 视频流  索引
    for (int i = 0; i < avFormatContext->nb_streams; i++) {
//视频流对象  avFormatContext->streams[i]  如果是视频
        if (avFormatContext->streams[i]->codecpar->codec_type==AVMEDIA_TYPE_VIDEO) {
            videoIndex = i;
//            所有的参数 包括音频 视频  AVCodecParameters
            AVCodecParameters *parameters = avFormatContext->streams[i]->codecpar;
            LOGI("视频%d", i);
            LOGI("宽度width:%d ", parameters->width);
            LOGI("高度height:%d ", parameters->height);
            LOGI("延迟时间video_delay  :%d ", parameters->video_delay);
//            实例化一个H264  全新解码  这里不能写死  根据视频文件动态获取
            AVCodec *dec = avcodec_find_decoder(parameters->codec_id);
//            根据解码器  初始化 解码器上下文
             videoContext= avcodec_alloc_context3(dec);
//             把读取文件里面的   参数信息 ，设置到新的上上下文
            avcodec_parameters_to_context(videoContext, parameters);
//        打开解码器
            avcodec_open2(videoContext, dec, 0);
        } else if (avFormatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            audioIndex = i;
            LOGI("音频%d", i);
        }
    }
    width = videoContext->width;
    height = videoContext->height;
    env->CallVoidMethod(instance, onSizeChange, width, height);
    nativeWindow= ANativeWindow_fromSurface(env, surface);
    ANativeWindow_setBuffersGeometry(nativeWindow, width, height, WINDOW_FORMAT_RGBA_8888);
//    开始实例化线程
//句柄
// 初始化容器
    rgbFrame = av_frame_alloc();
//    目前rgbFrame 初始化了   但是里面的容器没有初始化 ，告诉我容器代销
//    rgbFrame->data;  1  最小单元   1       1
    int numBytes = av_image_get_buffer_size(AV_PIX_FMT_RGBA, width, height, 1);
//实例化容器
    outbuffer = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));

//不能直接这样赋值
//    rgbFrame->data =outbuffer  下面的填充 本质和这种方式一样    多了宽高  显示格式
    av_image_fill_arrays(rgbFrame->data, rgbFrame->linesize, outbuffer, AV_PIX_FMT_RGBA, width,
                         height, 1);

    swsContext=sws_getContext(width, height, videoContext->pix_fmt,
                   width, height, AV_PIX_FMT_RGBA, SWS_BICUBIC, NULL, NULL, NULL);
    audioQueue = new MNQueue;
    videoQueue = new MNQueue;  //rgb  不支持yuv
    pthread_t thread_decode;
    pthread_t thread_vidio;
    isStart = true;
    pthread_create(&thread_decode, NULL, decodePacket, NULL);
    pthread_create(&thread_vidio, NULL, decodeVideo, NULL);

    env->ReleaseStringUTFChars(url_, url);

}