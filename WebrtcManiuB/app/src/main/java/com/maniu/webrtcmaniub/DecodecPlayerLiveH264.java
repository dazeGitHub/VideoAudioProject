package com.maniu.webrtcmaniub;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DecodecPlayerLiveH264 {
    private static final String TAG = "David";
    private MediaCodec mediaCodec;
    public void initDecoder(Surface surface) {
        try {
            mediaCodec = MediaCodec.createDecoderByType("video/avc");
            final MediaFormat format = MediaFormat.createVideoFormat("video/avc", 1080, 1920);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 1080 * 1920);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
            mediaCodec.configure(format,
                    surface,
                    null, 0);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void callBack(byte[] data) {
        Log.i(TAG, "接收到消息: " + data.length);
        int index= mediaCodec.dequeueInputBuffer(100000);
        if (index >= 0) {
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index);
            inputBuffer.clear();
            inputBuffer.put(data, 0, data.length);
            mediaCodec.queueInputBuffer(index,
                    0, data.length, System.currentTimeMillis(), 0);
        }
//        获取到解码后的数据  编码 ipbn
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
        while (outputBufferIndex >=0) {
            mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
    }

}
