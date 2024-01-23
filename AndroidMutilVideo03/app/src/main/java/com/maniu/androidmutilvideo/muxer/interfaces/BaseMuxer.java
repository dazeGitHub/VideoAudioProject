package com.maniu.androidmutilvideo.muxer.interfaces;

import android.media.MediaCodec;

import com.maniu.androidmutilvideo.encoder.StreamPublisherParam;
import com.maniu.androidmutilvideo.muxer.TimeIndexCounter;

public abstract class BaseMuxer implements IMuxer {

    protected TimeIndexCounter videoTimeIndexCounter = new TimeIndexCounter();
    protected TimeIndexCounter audioTimeIndexCounter = new TimeIndexCounter();

    @Override
    public int open(StreamPublisherParam params) {
        videoTimeIndexCounter.reset();
        audioTimeIndexCounter.reset();
        return 0;
    }

    @Override
    public void writeVideo(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
        videoTimeIndexCounter.calcTotalTime(bufferInfo.presentationTimeUs);
    }

    @Override
    public void writeAudio(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
        audioTimeIndexCounter.calcTotalTime(bufferInfo.presentationTimeUs);
    }
//    计算一个 帧        上一个编码时间戳
}
