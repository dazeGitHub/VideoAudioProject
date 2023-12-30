package com.maniu.rtmpmaniu;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Bundle;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoCodec extends  Thread{

    private MediaProjection mediaProjection;
    //    虚拟的画布
    private VirtualDisplay virtualDisplay;

    private MediaCodec mediaCodec;
    private long startTime;
    private ScreenLive screenLive;
    //    编码
    private boolean isLiving=false;
    public VideoCodec(ScreenLive screenLive) {
        this.screenLive = screenLive;
    }


    public void startLive(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
                720,
                1280);

        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

        format.setInteger(MediaFormat.KEY_BIT_RATE, 400_000);
//        帧率比较低     直播中I  250  400    视频  极限压缩  短视频 帧率   33帧      60帧  完美压缩了
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
//重新来一遍吗

        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");//手机
            mediaCodec.configure(format, null, null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);
            Surface surface = mediaCodec.createInputSurface();
            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "screen-codec",
                    720, 1280, 1,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    surface, null, null);

        } catch (IOException e) {
            e.printStackTrace();
        }
        isLiving = true;
        start();
    }
    private long timeStamp;
    @Override
    public void run() {

        mediaCodec.start();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (isLiving) {
            if (System.currentTimeMillis() - timeStamp >= 2000) {
                Bundle params = new Bundle();
                //立即刷新 让下一帧是关键帧 I 帧
                params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                mediaCodec.setParameters(params);
                timeStamp = System.currentTimeMillis();
            }
//            index 一定是  index >=0  sps 和pps  被编码几次
            int index = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
            if (index >= 0) {
                if (startTime == 0) {
                    // 微妙转为毫秒 dsp芯片 按照当时编码的系统时间 打出来的
                    // bufferInfo.presentationTimeUs 是绝对时间, 结果就是毫秒
                    startTime = bufferInfo.presentationTimeUs / 1000;
                }
                ByteBuffer buffer = mediaCodec.getOutputBuffer(index);
                byte[] outData = new byte[bufferInfo.size];
                buffer.get(outData);
//              FileUtils.writeBytes(outData);
//              FileUtils.writeBytes(outData) 写入了 h264文件, 可以再将 h264 文件转化为 MP4

                //当前时间 - 起始时间 就是 相对时间
                RTMPPackage rtmpPackage = new RTMPPackage(outData, (bufferInfo.presentationTimeUs / 1000) - startTime);
                rtmpPackage.setType(RTMPPackage.RTMP_PACKET_TYPE_VIDEO);
                screenLive.addPackage(rtmpPackage); //将 rtmpPackage 添加到 screenLive 的队列中
                mediaCodec.releaseOutputBuffer(index, false);
            }
//            起点
        }
        isLiving = false;
        mediaCodec.stop();
        mediaCodec.release();
        mediaCodec = null;
        virtualDisplay.release();
        virtualDisplay = null;
        mediaProjection.stop();
        mediaProjection = null;
    }
}
