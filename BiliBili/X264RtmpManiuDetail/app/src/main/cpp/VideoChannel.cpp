//
// Created by maniu on 2022/6/28.
//

#include <cstring>
#include "VideoChannel.h"

VideoChannel::VideoChannel() {

}

void VideoChannel::setVideoEncInfo(int width, int height, int fps, int bitrate) {
//    初始化 x264
    mWidth = width;
    mHeight = height;
    ySize = width * height;
    uvSize = ySize / 4;
    mFps = fps;
    mBitrate = bitrate;
//    定义参数 隐式声明
    x264_param_t  param ;
    //    参数赋值  ultrafast    编码器 速度   直播  越快 1  越慢2  zerolatency   编码质量   电影类型直播选用 zerolatency
    x264_param_default_preset(&param, "ultrafast", "zerolatency");
//    编码登记
    param.i_level_idc = 32;
//    选取显示格式
    param.i_csp = X264_CSP_I420;
    param.i_width = width;
    param.i_height = height;
    //    B帧
//    mediacodec b能编码   是1    小米11
    param.i_bframe = 0;
//折中    cpu   突发情况   ABR 平均
    param.rc.i_rc_method = X264_RC_ABR;
//k为单位  I帧  的比特
    param.rc.i_bitrate= bitrate / 1024;
    x264_encoder_open;

    //帧率   1s/25帧
//    分母
    param.i_fps_num = fps;
//    分子
    param.i_fps_den = 1;

//    1s/帧率 =时间

//帧率     1s/时间

//    分母
    param.i_timebase_den=param.i_fps_num;
//    分子
    param.i_timebase_num = param.i_fps_den;

    param.b_vfr_input= 0;
//    秒开  i帧 2500   1s
    param.i_keyint_max = 2 * fps;

// sps pps 是不是重复输出 1     0 输出一次
    param.b_repeat_headers = 1;

    //多线程
    param.i_threads = 1;

    x264_param_apply_profile(&param, "baseline");
    videoCodec = x264_encoder_open(&param);
    pic_in = new x264_picture_t;
    x264_picture_alloc(pic_in, X264_CSP_I420, width, height);
//    初始化搞定了  简单
}

VideoChannel::~VideoChannel() {

}

void VideoChannel::encodeData(int8_t *data) {
//    x264编码
    memcpy(pic_in->img.plane[0], data, ySize);
    for (int i = 0; i < uvSize; ++i) {
//u 1   v 2  //u数据
        *(pic_in->img.plane[1] + i) = *(data + ySize + i * 2 + 1);
        //v数据
        *(pic_in->img.plane[2] + i) = *(data + ySize + i * 2);
    }
    //编码出的数据 H264
    x264_nal_t *pp_nals;
    //编码出了几个 nalu （暂时理解为帧）  1   pi_nal  1  永远是1
    int pi_nal;
//编码出的参数  BufferInfo
    x264_picture_t pic_out;
    //编码出的数据 H264
    x264_encoder_encode(videoCodec, &pp_nals, &pi_nal, pic_in, &pic_out);
//借助java  --》文件
}
