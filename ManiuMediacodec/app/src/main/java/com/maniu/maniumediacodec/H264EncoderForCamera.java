package com.maniu.maniumediacodec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.io.IOException;
import java.nio.ByteBuffer;

//CameraActivity 摄像头用的
public class H264EncoderForCamera {
    MediaCodec mediaCodec;
    int index;
    int width;
    int height;
    public H264EncoderForCamera(int width, int height) {
        this.width = width;
        this.height = height;
    }
    public void startLive()  {
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
            MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2); //IDR帧 (I 帧) 刷新时间
            //现在不是通过 Surface 传的, 而是通过 byte[] 数组传过来的, 所以使用 COLOR_FormatYUV420Flexible
            //MediaCodecInfo.CodecCapabilities 是没有 nv21 的
            //安卓摄像头默认是 nv21, 但是谷歌只支持 nv12, 这里传的也是 COLOR_FormatYUV420Flexible 即 nv12,
            //如果不做处理那么 yuv 中的 uv 就无法正常显示色度, 导致画面是黑白的
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    input  固定1  不是 2
    public int encodeFrame(byte[] input) {
//      input.length 是固定的,

//      输入
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(10000);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer =   mediaCodec.getInputBuffer(inputBufferIndex);
            inputBuffer.clear();
            inputBuffer.put(input);
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, computePts(), 0);
            index++;
        }

//      输出
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo,100000);
        if (outputBufferIndex >= 0) {
            ByteBuffer  outputBuffer= mediaCodec.getOutputBuffer(outputBufferIndex);
            byte[] data = new byte[bufferInfo.size];
            outputBuffer.get(data);
            FileUtils.writeBytes(data);
            FileUtils.writeContent(data);
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
        }
        return -1; //return 其实没有意义
    }

//  单位使用微秒而不是毫秒
//  计算帧率 1000 000 微妙 15帧, 那么 第 1 帧  1000 000 / 15, 第二帧就是 1000 000 / 15 * 2
//  第三帧就是 1000 000 /15 * 3, 以此类推  1000 000 /15 * 4   1000 000 /15 * 5
    public int computePts() {
        return 1000_000 / 15 * index;
    }
}
