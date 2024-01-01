//
// Created by maniu on 2022/6/28.
//

#include <cstring>
#include "VideoChannel.h"

VideoChannel::VideoChannel() {

}

void VideoChannel::setVideoEncInfo(int width, int height, int fps, int bitrate) {
//  初始化 x264
//  将这些变量保存为全局变量
    mWidth = width;
    mHeight = height;
    ySize = width * height;
    uvSize = ySize / 4;
    mFps = fps;
    mBitrate = bitrate;

//  定义参数 隐式声明对象
    x264_param_t  param ;
//  参数赋值  ultrafast    编码器 速度   直播  越快 1  越慢2  zerolatency   编码质量   电影类型直播选用 zerolatency
//  第二个参数 : preset 参数 表示编码速度, 从 x264_preset_names 数组中取一个, 从左到右是从快到慢, ultrafast 是最快
//  第三个参数 : tune 参数 表示编码质量, 有 8 个, 用来调节画质, 可以从 x264_tune_names 数组中取一个然后赋值, zerolatency 表示零延迟 (适用于直播)
//  具体参考: https://blog.csdn.net/lj501886285/article/details/105103975
    x264_param_default_preset(&param, "ultrafast", "zerolatency");
//  编码等级(最大支持码流范围), 参考 G:\教程\码牛学院\音视频\9. 从零手写音视频会议项目 (钉钉视频会议实战)\资料\资料.md
//  i_level_idc 为 32 表示 55296000s 每秒
    param.i_level_idc = 32;
//  选取显示格式
    param.i_csp = X264_CSP_I420;
    param.i_width = width;
    param.i_height = height;
//  mediacodec 可以编码 b 帧, 条件是不是所有手机都支持, 例如 小米11 以后才支持
//  i_bframe 表示在两个 I 帧直接编码多少个 B 帧
    param.i_bframe = 0; //0 表示不编码 B 帧

//  i_rc_method 表示 CPU 如何使用, 参考 https://blog.51cto.com/u_15127582/3290733
//  X264_RC_CQP(恒定质量, 保证质量恒定大不了延长时间)   X264_RC_CRF(只管速度不管质量)  X264_RC_ABR(居中, 在 X264_RC_CQP 和 X264_RC_CRF 之间)
    param.rc.i_rc_method = X264_RC_ABR;

//  k为单位  I帧  的比特, 所以是最大比特位除以 1024
    param.rc.i_bitrate= bitrate / 1024;

//  这是一个宏函数, create a new encoder handler, all parameters from x264_param_t are copied
    x264_encoder_open;

//  i_fps_num 表示帧率 = 单位时间 1s/25帧
//  帧率和时间互为倒数, 1s / 帧率 = 时间, 帧率 = 1s / 时间
//  分母
    param.i_fps_num = fps;
//  分子 1s
    param.i_fps_den = 1;

//  分母
    param.i_timebase_den = param.i_fps_num;
//  分子
    param.i_timebase_num = param.i_fps_den;

//  b_vfr_input 的意思是变动帧率输入, 这样编码器计算帧之间的 duration, 直接用前后帧时间戳相减
//  一般不需要配置该变量
    param.b_vfr_input = 0;

//  这个参数很重要, 表示 I 帧的间隔, 秒开决定的因素就是 i帧 的间隔, 一般设置为 2 秒
    param.i_keyint_max = 2 * fps;

//  mediaCodec 中 sps 和 pps 只输出一次, 使用 x264 软编的话, b_repeat_headers 表示 sps pps 是不是重复输出, 1 是重复, 0 是只输出一次
//  sps 被 x264 缓存了, 不用自己缓存
    param.b_repeat_headers = 1;

    //多线程
    param.i_threads = 1;

//  profile 编码等级有 : Baseline(直播), Main(一般场景), Extended, High(FRExt), High 10(FRExt), High 4:2:2(FRExt), High 4:4:4(FRExt)
    x264_param_apply_profile(&param, "baseline");
    videoCodec = x264_encoder_open(&param);

    pic_in = new x264_picture_t; //x264_picture_t 中有 x264_image_t 这是一帧图像, x264_image_t 中有四个通道 (可能是 yuv 也可能是 argb)
//  X264_CSP_I420 表示显示格式 yuv420
    x264_picture_alloc(pic_in, X264_CSP_I420, width, height);
}

//videoChannel 对象 delete 时会触发析构函数
VideoChannel::~VideoChannel() {

}

//将数据放到 x264_picture_t 中的 yuv 里面
void VideoChannel::encodeData(int8_t * data) {
//  x264编码
    memcpy(pic_in -> img.plane[0], data, ySize);
    for (int i = 0; i < uvSize; ++i) {
        //NV12 先U 后V
        //奇数是 u 数据
        *(pic_in -> img.plane[1] + i) = *(data + ySize + i * 2 + 1);
        //偶数是 v 数据
        *(pic_in -> img.plane[2] + i) = *(data + ySize + i * 2);
    }

    //编码出的数据 H264
    x264_nal_t * pp_nals;
    //编码出了几个 nalu （暂时理解为帧）  1   pi_nal  1  永远是1
    int pi_nal;
//  编码出的参数  BufferInfo
    x264_picture_t pic_out;
    //编码出的数据 H264
    //pp_nals 是宏块单元, pi_nal 是宏块数组, pic_in 是输入的数据, pic_out 是输出的数据
    //真正编码出的数据是放到 pp_nals 里面
    x264_encoder_encode(videoCodec, &pp_nals, &pi_nal, pic_in, &pic_out);
//  借助java  --》文件
}
