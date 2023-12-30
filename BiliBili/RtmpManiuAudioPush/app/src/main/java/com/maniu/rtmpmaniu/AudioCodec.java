package com.maniu.rtmpmaniu;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

//视频编码和音频编码都是一样的, 使用 MediaCodec
public class AudioCodec extends Thread {
    private static final String TAG = "David";
    private MediaCodec mediaCodec;
    //传输层
    private int minBufferSize;
    private ScreenLive screenLive;
    private boolean isRecoding;
    private AudioRecord audioRecord;
    private long startTime;
    public AudioCodec(ScreenLive screenLive) {
        this.screenLive = screenLive;
    }

// 播放  ---》 音频  能够正常解析  出现噪音主要是配置错了    例如明明是  44100  结果推流过来是 48000
    @SuppressLint("MissingPermission")
    public void startLive() {
        try {
            //MediaFormat.MIMETYPE_AUDIO_AAC 就是 audio/mp4a-latm
            MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 1);
//          录音质量
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
//          一秒的码率 aac
            format.setInteger(MediaFormat.KEY_BIT_RATE, 128_000);
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//          通道数  采样位数   采样频率
            minBufferSize = AudioRecord.getMinBufferSize(
  44100, //采样频率
                AudioFormat.CHANNEL_IN_MONO,   //通道数 单声道
                AudioFormat.ENCODING_PCM_16BIT //采样位数
            );
            mediaCodec.start();
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC, //MIC 麦克风  VOICE_CALL 上麦克风,  CAMCORDER 喇叭
                44100,
                AudioFormat.CHANNEL_IN_MONO,   //通道数 单声道
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        start();
    }

    //    视频编码 和 音频   编码 是1  不是2
    @Override
    public void run() {
        isRecoding = true;
        audioRecord.startRecording();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        RTMPPackage rtmpPackage = new RTMPPackage();
        byte[] audioDecoderSpecificInfo = {0x12, 0x08};//让服务器准备好, 要发送音频数据了
        rtmpPackage.setBuffer(audioDecoderSpecificInfo);
        rtmpPackage.setType(RTMPPackage.RTMP_PACKET_TYPE_AUDIO_HEAD);
        screenLive.addPackage(rtmpPackage);

        byte[] buffer = new byte[minBufferSize];

        while (isRecoding) {
//          这里得到原始数据 buffer
//          将录音数据读取到 buffer 中
            int len = audioRecord.read(buffer, 0, buffer.length);

//          pcm 数据编码
            if (len <= 0) {
                continue;
            }
            //立即得到有效输入缓冲区
            int index = mediaCodec.dequeueInputBuffer(1000);
            if (index >= 0) {
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index);
                inputBuffer.clear();
                Log.i(TAG, "run: len  " + len);
//                周一讲bug
                inputBuffer.put(buffer, 0, len);
                //填充数据后再加入队列
                mediaCodec.queueInputBuffer(index, 0, len, System.nanoTime() / 1000, 0);
            }
            index = mediaCodec.dequeueOutputBuffer(bufferInfo, 1000);

            while (index >= 0 && isRecoding) {
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(index);
                byte[] outData = new byte[bufferInfo.size];
                outputBuffer.get(outData); //编码好的数据
                if (startTime == 0) {
                    startTime = bufferInfo.presentationTimeUs / 1000;
                }
                rtmpPackage = new RTMPPackage();
                rtmpPackage.setBuffer(outData);
//              native    音频数据
                rtmpPackage.setType(RTMPPackage.RTMP_PACKET_TYPE_AUDIO_DATA);
//              关键是设置时间戳
                long tms = (bufferInfo.presentationTimeUs / 1000) - startTime;
                rtmpPackage.setTms(tms);
                screenLive.addPackage(rtmpPackage);
                mediaCodec.releaseOutputBuffer(index, false);
                index = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            }
        }
    }
}
