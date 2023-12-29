package com.maniu.maniuwebrtcroom;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
//10个人     解码器  9, 数据流不同, 所以要使用不同的多个解码器
//多解码器 100 个
public class DecodecPlayerLiveH264 {
    private String remoteIp;
    private MediaCodec mediaCodec;

    public void initDecoder(String remoteIp, Surface surface) {
        this.remoteIp = remoteIp;
        try {
            mediaCodec = MediaCodec.createDecoderByType("video/avc");
            final MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 720, 1280);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 720 * 1280);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            mediaCodec.configure(format, surface, null, 0);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public void drawSurface(byte[] data) {
//        解码  cpu--> dsp   dsp---cpu
        int index= mediaCodec.dequeueInputBuffer(100000);
        if (index >= 0) {
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index);
            inputBuffer.clear();
            //data  压缩  1  未压缩2
            inputBuffer.put(data, 0, data.length);
//            dsp芯片解码
            mediaCodec.queueInputBuffer(index,
                    0, data.length, System.currentTimeMillis(), 0);
        }
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
        if (outIndex >= 0) {
            mediaCodec.releaseOutputBuffer(outIndex, true);
        }
    }
}
