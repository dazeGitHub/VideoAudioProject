package com.maniu.webrtcmaniua;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.io.IOException;
import java.nio.ByteBuffer;

public class EncodecPushLiveH264 {
    private MediaCodec mediaCodec;
//核心
    int width;
    int height;
    int frameIndex;
    byte[] nv12;
    byte[] yuv;
    public static final int NAL_I = 0x5;
    public static final int NAL_SPS = 0x7;
    private byte[] sps_pps_buf;
    private SocketLive socketLive;
    public EncodecPushLiveH264( SocketLive.SocketCallback socketCallback, int width, int height) {
        this.socketLive  = new SocketLive( socketCallback);
        socketLive.start();
        this.width = width;
        this.height = height;
    }
    public SocketLive getSocketLive() {
        return socketLive;
    }

    public void startLive() {
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
            MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc",height,width);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1080 * 1920);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5); //IDR帧刷新时间
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
            int bufferLength = width*height*3/2;
            yuv = new byte[bufferLength];
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
//    一帧数据 过来
    public void encodeFrame(byte[] input) {
//        nv21-->nv12
        nv12 = YuvUtils.nv21toNV12(input);
//放
        YuvUtils.portraitData2Raw(nv12, yuv, width, height);
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(100000);
        if (inputBufferIndex >= 0) {
            ByteBuffer[] byteBuffers = mediaCodec.getInputBuffers();
            ByteBuffer inputBuffer = byteBuffers[inputBufferIndex];
            inputBuffer.clear();
//            原始数据1   压缩2
            inputBuffer.put(yuv);
//dsp芯片解码
            long presentationTimeUs = computePresentationTime(frameIndex);
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, yuv.length, presentationTimeUs, 0);
            frameIndex++;
        }
//        cpu数据---》dsp   编码好

//        cpu  直播   通话 剪辑

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
        if (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
//            byte[] data = new byte[bufferInfo.size];
//            outputBuffer.get(data);

//            YuvUtils.writeBytes(data);
//            YuvUtils.writeContent(data);
            dealFrame(outputBuffer, bufferInfo);
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
        }


    }
    private void dealFrame(ByteBuffer bb, MediaCodec.BufferInfo bufferInfo) {
//00 00  00 01
//00 00 01
        int offset = 4;
        if (bb.get(2) == 0x01) {
            offset = 3;
        }
//    sps
        int type = (bb.get(offset) & 0x1F) ;
//        有 1  没有2  type=7
        if (type == NAL_SPS) {
//            不发送       I帧
            sps_pps_buf = new byte[bufferInfo.size];
            bb.get(sps_pps_buf);
        }else if (type == NAL_I) {
            final byte[] bytes = new byte[bufferInfo.size];
            bb.get(bytes);
//            bytes           I帧的数据
            byte[] newBuf = new byte[sps_pps_buf.length + bytes.length];
            System.arraycopy(sps_pps_buf, 0, newBuf, 0, sps_pps_buf.length);
            System.arraycopy(bytes, 0, newBuf, sps_pps_buf.length, bytes.length);
//            编码层   推送出去
            socketLive.sendData(newBuf,1);

        }else {
            final byte[] bytes = new byte[bufferInfo.size];
            bb.get(bytes);
            this.socketLive.sendData(bytes,1);
        }
    }



    private long computePresentationTime(long frameIndex) {
        return  frameIndex * 1000000 / 15;
    }

}
