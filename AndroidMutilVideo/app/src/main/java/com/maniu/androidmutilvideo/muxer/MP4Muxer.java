package com.maniu.androidmutilvideo.muxer;

import android.media.MediaCodec;

import com.maniu.androidmutilvideo.encoder.StreamPublisherParam;
import com.maniu.androidmutilvideo.muxer.interfaces.IMuxer;

public class MP4Muxer implements IMuxer {
    @Override
    public int open(StreamPublisherParam params) {
        return 0;
    }

    @Override
    public void writeVideo(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {

    }

    @Override
    public void writeAudio(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {

    }

    @Override
    public int close() {
        return 0;
    }
}
