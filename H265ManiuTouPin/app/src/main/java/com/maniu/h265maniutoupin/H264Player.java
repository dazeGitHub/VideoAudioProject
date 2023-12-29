package com.maniu.h265maniutoupin;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class H264Player implements SocketLive.SocketCallback {
    private static final String TAG = "H264Player";
    private MediaCodec mediaCodec;
    public H264Player(Surface surface) {
        try {
            //h264 和 h265 是一样的, 都是 MediaFormat.MIMETYPE_VIDEO_AVC
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            final MediaFormat format = MediaFormat.
                    createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 720, 1280);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 720 * 1280);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            mediaCodec.configure(format,
                    surface,
                    null, 0);
            mediaCodec.start();
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }

    //  收到并处理消息, 是在该 callBack() 方法中
    //  bytes 就是一帧数据
    @Override
    public void callBack(byte[] bytes) {
        //解码器前长度 都是不同的, 解码后的长度都是相同的
        Log.i(TAG, "解码器前长度  : " + bytes.length);
        int index = mediaCodec.dequeueInputBuffer(100000);
        if (index >= 0) {
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index);
            inputBuffer.clear();
            inputBuffer.put(bytes, 0, bytes.length);
            mediaCodec.queueInputBuffer(index,
                    0, bytes.length, System.currentTimeMillis(), 0);
        }
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
//       输入 100k   输出可能大于 4M
//       放一个完整帧  ---》     渲染出来
//        if (outputBufferIndex >= 0) {
//            mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
//        }
        Log.i(TAG, "解码器后长度  : " + bufferInfo.size); //为什么我解码后长度是 1, 老师是 1382400
//      解码视频帧的时候一次可能解码不完, 可能要多次解码, 所以使用 while 而不使用 if
        while (outputBufferIndex >= 0) {
            mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
    }
}
