package com.maniu.webrtcmaniub;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.FileInputStream;

public class AudioRecoderLive {
    private AudioTrack audioTrack;
    SocketLive socketLive;
    AudioRecord audioRecord;

    // 采样率，现在能够保证在所有设备上使用的采样率是44100Hz, 但是其他的采样率（22050, 16000, 11025）在一些设备上也可以使用。
    public static final int SAMPLE_RATE_INHZ = 44100;

    //声道数。CHANNEL_IN_MONO (双通道) and CHANNEL_IN_STEREO. 其中 CHANNEL_IN_MONO 是可以保证在所有设备能够使用的。
    //CHANNEL_OUT_5POINT1  是 5.1 环绕,  CHANNEL_OUT_5POINT1POINT2 是 5.2 环绕,  CHANNEL_OUT_5POINT1POINT4 是 5.4 环绕
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;

    // 采样位数
    // 返回的音频数据的格式。 ENCODING_PCM_8BIT, ENCODING_PCM_16BIT, and ENCODING_PCM_FLOAT.
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

//    int bufferSizeInBytes
    boolean isRecording = false;
    HandlerThread handlerThread;
    Handler workHandler;
    public AudioRecoderLive(SocketLive socketLive) {
        this.socketLive = socketLive;
    }
    //    2通道     44100   16   能
//     1 通道     44100   16   能  录制端     播放端

    public void startRecord(Context context) {
        handlerThread = new HandlerThread("handlerThread");
        handlerThread.start();
        workHandler = new Handler(handlerThread.getLooper());
        final int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_INHZ, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "没有录音权限", Toast.LENGTH_SHORT).show();
            return;
        }
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE_INHZ,
                CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize);

        audioRecord.startRecording();
        isRecording = true;
        // 初始化缓存
        final byte[] data = new byte[minBufferSize];
        // 创建数据流，将缓存导入数据流
        workHandler.post(new Runnable() {
            @Override
            public void run() {
                while (isRecording) {
                    int length = audioRecord.read(data, 0, minBufferSize);
                    YuvUtils.writeContent(data);
                    YuvUtils.writeBytes(data);
                    socketLive.sendData(data, 0);
                }
            }
        });
    }
    public void initPlay() {
        Log.i("Tag8","go there");
        //配置播放器
        //音乐类型,扬声器播放
        int streamType = AudioManager.STREAM_MUSIC;
        //录音时采用的采样频率,所有播放时同样的采样频率
        int sampleRate = SAMPLE_RATE_INHZ;
        //单声道,和录音时设置的一样
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        // 录音使用16bit,所有播放时同样采用该方式
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        //流模式
        int mode = AudioTrack.MODE_STREAM;

        //计算最小buffer大小
        int minBufferSize=AudioTrack.getMinBufferSize(sampleRate,channelConfig,audioFormat);

        //构造AudioTrack  不能小于AudioTrack的最低要求，也不能小于我们每次读的大小
        audioTrack=new AudioTrack(streamType,sampleRate,channelConfig,audioFormat,
                minBufferSize,mode);
        audioTrack.setVolume(16f);
        //从文件流读数据
        FileInputStream inputStream = null;
        audioTrack.play();
    }

    public void doPlay(byte[] buffer) {
        int ret = audioTrack.write(buffer, 1, buffer.length-1);
        Log.i("Tag8","ret ==="+ret);
        //检查write的返回值,处理错误
        switch(ret) {
            case AudioTrack.ERROR_INVALID_OPERATION:
            case AudioTrack.ERROR_BAD_VALUE:
            case AudioManager.ERROR_DEAD_OBJECT:
                return;
            default:
                break;
        }
        Log.i("Tag8","播放成功。。。。");
    }
}
