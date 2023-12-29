package com.maniu.maniumediacodec;

import static android.media.MediaCodec.CONFIGURE_FLAG_ENCODE;

import android.hardware.display.DisplayManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

//RecordScreenActivity 录屏专用的
public class H264EncoderForRS extends Thread{
//    数据源
    private MediaProjection mMediaProjection;
    private int width;
    private int height;
//    编码器
    MediaCodec mediaCodec;
    // 输出 文件

    public H264EncoderForRS(MediaProjection mMediaProjection) {
        this.mMediaProjection = mMediaProjection;
//        解码  ======》json----》 自身提供了 你想要的信息 jiema
        this.width = 640;   //720
        this.height = 1920; //1280
//        编码   -----》 输出json  基本  款考 sps pps 解码   json
//        编码   json
        //MediaFormat.MIMETYPE_VIDEO_AVC 就是 "video/avc" - H.264/AVC video
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);

        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");

//            1s  20  david   生产黄豆       20个黄豆  每秒     每隔30个数量  绿豆
//            GOP   很长 -----》   30 I强制  直播  强制
//
            //这里的配置会影响 sps 和 pps
            //帧率
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 20); //50

//          30帧    一个I帧
//          I 帧的间隔
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 30); //150

//          码率     width height  帧  码率    zip   txt 100k  zip（h264）    50k    20k
//          码率越高, 质量越清晰
            format.setInteger(MediaFormat.KEY_BIT_RATE, width * height);

//          来源, 编码一定要传
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

//          第二个参数解码时传 surface 来显示, 编码的时候不需要传
//          第三个参数是加密
//          第四个参数是 编码标志位, 解码时传 0 (默认), 编码传 CONFIGURE_FLAG_ENCODE
            mediaCodec.configure(format,null,null, CONFIGURE_FLAG_ENCODE);

//          mediaCodec  david   ---》场地 越大   1  越小 2        1dp-----
            Surface surface = mediaCodec.createInputSurface(); //之前的 Surface 是来自 SurfaceView, 这里是 mediaCodec 创建的

//          因为不需要显示, 所以使用 mMediaProjection 创建虚拟的屏幕
//          将 mMediaProjection 的数据放到 surface 里

//          第一个参数 : name 随便传一个就行, 做为 VirtualDisplay 的名字
//          第四个参数 : dpi 为 2 表示 一个 dpi 等于 2 个像素, 该值越大越清晰
//          第五个参数 : flag, 表示关系是公开还是私有
//          第七个参数 : callback, 用户关闭录屏什么的时候回调
//          第八个参数 : handler, 传递 handler 进去, 那么 VirtualDisplay 就会通过 Handler 给开发者发消息
            mMediaProjection.createVirtualDisplay(
                    "jett-david", width, height, 2,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    surface, null, null
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        super.run();
        mediaCodec.start();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        while (true) {
            //直接拿到输出, 不用管输入, 输入已经被实现了
            int outIndex = mediaCodec.dequeueOutputBuffer(info, 10000);
            if (outIndex >= 0) {
//              编码的数据
                ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outIndex);
                byte[] ba = new byte[byteBuffer.remaining()]; //byteBuffer.remaining() 等同于 info.size()
                byteBuffer.get(ba);//将容器的byteBuffer  内部的数据 转移到 byte[]中
                //用两种方式写文件, 一种是 bytes 方式, 一种是 将字节转换为 16 进制的方式
                FileUtils.writeBytes(ba);
                FileUtils.writeContent(ba);
                //编码不需要渲染, 所以传 false, 如果需要渲染, 之前就应当配置 surface 然后这里传 true
                mediaCodec.releaseOutputBuffer(outIndex, false);
            }
        }
    }
}
