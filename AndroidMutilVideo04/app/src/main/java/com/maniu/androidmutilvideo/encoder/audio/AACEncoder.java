package com.maniu.androidmutilvideo.encoder.audio;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import com.maniu.androidmutilvideo.encoder.MediaCodecInputStream;
import com.maniu.androidmutilvideo.encoder.StreamPublisherParam;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AACEncoder {
    private static final String TAG = "AACEncoder";
    private AudioRecord mAudioRecord;
    private MediaCodec mMediaCodec;
    //    流   音频流
    private MediaCodecInputStream mediaCodecInputStream;

    private Thread mThread;
    private int samplingRate;
    private int bufferSize;
    private boolean isStart;

    private OnDataComingCallback onDataComingCallback;

    @SuppressLint("MissingPermission")
    public AACEncoder(StreamPublisherParam params) {
        this.samplingRate = params.samplingRate;

        bufferSize = params.audioBufferSize;
        try {
            mMediaCodec = MediaCodec.createEncoderByType(params.audioMIME);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaCodec.configure(params.createAudioMediaFormat(), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodecInputStream = new MediaCodecInputStream(mMediaCodec, new MediaCodecInputStream.MediaFormatCallback() {
            @Override
            public void onChangeMediaFormat(MediaFormat mediaFormat) {
                params.setAudioOutputMediaFormat(mediaFormat);
            }
        });
//        录音器  数据    视频 GPU    拿不到
        mAudioRecord = new AudioRecord(params.audioSource, samplingRate,
                params.channelCfg, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
    }
    public void start() {
//
        final long startWhen = System.nanoTime();
        mAudioRecord.startRecording();
        mMediaCodec.start();
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int len, bufferIndex;
                while (isStart && !Thread.interrupted()) {
                    synchronized (mMediaCodec) {
                        if (!isStart) return;
//
//可以用的容器
                        bufferIndex = mMediaCodec.dequeueInputBuffer(10000);
                        if (bufferIndex >= 0) {

                            long presentationTimeNs = System.nanoTime();
                            ByteBuffer byteBuffer = mMediaCodec.getInputBuffer(bufferIndex);
                            byteBuffer.clear();
//                            从录音器 外放  你要哪里   inputBuffers[bufferIndex]  500k  125k
//                            cpu  1   dsp  2
                            len =  mAudioRecord.read(byteBuffer, bufferSize);
                            long presentationTimeUs = (presentationTimeNs - startWhen) / 1000;


                            if (len == AudioRecord.ERROR_INVALID_OPERATION
                                    || len == AudioRecord.ERROR_BAD_VALUE) {
                                Log.e(TAG, "An error occured with the AudioRecord API !");
                            } else {
                                mMediaCodec.queueInputBuffer(bufferIndex, 0, len, presentationTimeUs, 0);
                                if (onDataComingCallback != null) {
                                    onDataComingCallback.onComing();
                                }
                            }
                        }

                    }



                }

            }
        });

//------------------开启音频线程-----bug--------------
        mThread.start();
        isStart = true;
    }
    public void setOnDataComingCallback(OnDataComingCallback onDataComingCallback) {
        this.onDataComingCallback = onDataComingCallback;
    }
    public interface OnDataComingCallback {
        void onComing();
    }
//    关闭
    public synchronized void close() {
        if (!isStart) {
            return;
        }
        isStart = false;

        mThread.interrupt();
        mediaCodecInputStream.close();

        synchronized (mMediaCodec) {
            mMediaCodec.stop();
            mMediaCodec.release();
        }

        mAudioRecord.stop();
        mAudioRecord.release();

    }
    public MediaCodecInputStream getMediaCodecInputStream() {
        return mediaCodecInputStream;
    }
}
