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
            //MediaFormat.MIMETYPE_AUDIO_AAC 就是 audio/mp4a-latm     //44100 必须双通道, 这里通道数传 2 是为了让 mediaCodec 的 inputBuffer 变大一些
            MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2);
//          录音质量, 如果录音质量比较低, 那么 dsp 芯片就会默认开启一个低容量的 buffer
//          AACObjectLC 非常低, AACObjectLD 比较低, AACObjectLTP 高, AACObjectMain 主流的, AACObjectSSR 高清
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectMain);
            //配置下面声音就不嘈杂了
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100);
            //B 站后端的设计只支持单通道, 即使录制的时候是双通道, 双通道推送到服务器单通道也没有问题, 抛弃一个通道即可
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 32 * 1024);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8820); //随便填, 一般超过 4000

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
                //AudioFormat.CHANNEL_IN_MONO 是单声道    CHANNEL_IN_STEREO 是双声道
                // 如果前面 channel 数设置为 1, 这里可以设置 CHANNEL_IN_LEFT 或 CHANNEL_IN_RIGHT, 可以试试
                AudioFormat.CHANNEL_IN_MONO,
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

//      buffer 是通过 音频麦克风实例化的
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
                //inputBuffer 是通过 mediaCodec 实例化的
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
