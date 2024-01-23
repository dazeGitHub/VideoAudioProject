package com.maniu.androidmutilvideo.muxer.interfaces;

import android.media.MediaCodec;

import com.maniu.androidmutilvideo.encoder.StreamPublisherParam;

public interface IMuxer
{

    int open(StreamPublisherParam params);

    void writeVideo(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo);

    void writeAudio(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo);

    int close();

}
