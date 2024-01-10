#include <jni.h>
#include <string>
#include <android/log.h>
#include <pthread.h>
//ffmpeg 的代码都是 c 语言写的, 所以导入头文件必须加 extern "C"
extern "C"{
#include <libavformat/avformat.h>
#include "libavcodec/avcodec.h"
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
//MNQueue *videoQueue;
////音频队列
//MNQueue *audioQueue;
//ffmpeg  c 代码

AVCodecContext *videoContext;
AVFormatContext *avFormatContext;

bool isStart = false;

extern "C" JNIEXPORT jstring JNICALL
Java_com_maniu_maniuijk_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject MainActivity
) {
    std::string hello = "Hello from C++";
    return env ->NewStringUTF(av_version_info());
}

void *decodeVideo(void *pVoid) {
    LOGI("==========解码线程");
    while (isStart) {
        AVPacket *videoPacket = av_packet_alloc();
//        videoQueue->get(videoPacket)
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
        AVPacket *avPacket = av_packet_alloc();
        int ret = av_read_frame(avFormatContext, avPacket);//压缩数据
        if (ret < 0) {
//            文件末尾
            break;
        }
        if (avPacket->stream_index == videoIndex) {
//视频包
            LOGD("视频包 %d", avPacket->size);
//            videoQueue->push(avPacket);
        }else  if(avPacket->stream_index == audioIndex) {
//视频包


        }
    }

    return NULL;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_maniu_maniuijk_MNPlayer_play(JNIEnv *env, jobject thiz, jstring url_, jobject surface) {
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
//            实例化一个H264  全新解码
            AVCodec *dec = avcodec_find_decoder(AV_CODEC_ID_H264);
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


//    开始实例化线程
//句柄
//    audioQueue = new MNQueue;
//    videoQueue = new MNQueue;
    pthread_t thread_decode;
    pthread_t thread_vidio;
    isStart = true;
    pthread_create(&thread_decode, NULL, decodePacket, NULL);
    pthread_create(&thread_vidio, NULL, decodeVideo, NULL);

    env->ReleaseStringUTFChars(url_, url);

}