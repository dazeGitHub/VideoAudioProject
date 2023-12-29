package com.maniu.maniuwebrtcroom;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class EncoderPlayerLiveH264 {
    private SocketLive socketLive;
//    硬编码  软编码有什么区别

    int width;
    int height;
    byte[] yuv;
    int frameIndex = 0;

    //    视频编码的 软编
    private MediaCodec mediaCodec;

    public EncoderPlayerLiveH264(int width, int height) {
        this.width = width;
        this.height = height;
    }

    //初始化方法
    public void startCapture(SocketLive socketLive) {
        this.socketLive = socketLive;
        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, height, width);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1080 * 1920);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5); //IDR帧刷新时间
//有1  没有  2
//            军工 的视频通话  不能 够  路由  加密的音视频 通话   音视频会议
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

//    mediaCodec  编码原理

    //    编码的方法 input2 M
    public void encodeFrame(byte[] input) {
//cpu 前置处理
//        数据  nv21

        byte[] nv12 = YuvUtils.nv21toNV12(input);
//
        if (yuv == null) {
            yuv = new byte[nv12.length];
        }
        YuvUtils.portraitData2Raw(nv12, yuv, width, height);
//        dsp  --->

//格式   H264  25帧    40ms  *index
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(3000);
//        弃用
//        ByteBuffer[] byteBuffers= mediaCodec.getInputBuffers();
        if (inputBufferIndex >= 0) {
//            技师了
//             ByteBuffer byteBuffer = byteBuffers[inputBufferIndex];
            ByteBuffer byteBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
            byteBuffer.put(yuv);
//            通知的意思
            long presentationTimeUs = computePresentationTime(frameIndex);
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, yuv.length, presentationTimeUs, 0);
            frameIndex++;
        }
//        3s  30min

//        多少不固定    等于2M  小于2M  编码小于   解码 是大于
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 3000);

        if (outputBufferIndex >= 0) {
//            小姐姐
            ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
            byte[] ba = new byte[bufferInfo.size];
//            dsp---cpu
//            outputBuffer.get(ba);
//            FileUtils.writeBytes(ba);
            dealFrame(outputBuffer, bufferInfo);
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
        }
    }
    private long computePresentationTime(long frameIndex) {
        return frameIndex * 1000000 / 15;
    }
    //    spspps
    private byte[] configBuf;

    private void dealFrame(ByteBuffer bb, MediaCodec.BufferInfo bufferInfo) {
        int type = bb.get(4);

        if (type == 0x67) {
            configBuf = new byte[bufferInfo.size];
            bb.get(configBuf);
        } else if (type == 0x65) {
//配置帧  当前的I帧一起发出去       配置帧
            final byte[] Ibytes = new byte[bufferInfo.size];
            bb.get(Ibytes);
//新容器
            byte[] newBuf = new byte[configBuf.length + Ibytes.length];
            System.arraycopy(configBuf, 0, newBuf, 0, configBuf.length);

            System.arraycopy(Ibytes, 0, newBuf, configBuf.length, Ibytes.length);
//            网络层发出去
            socketLive.sendData(newBuf);
            Log.d("David", "视频数据  " + Arrays.toString(Ibytes));
        } else {
            final byte[] bytes = new byte[bufferInfo.size];
            bb.get(bytes);
            socketLive.sendData(bytes);
            Log.d("David", "视频数据  " + Arrays.toString(bytes));
        }
    }
}
