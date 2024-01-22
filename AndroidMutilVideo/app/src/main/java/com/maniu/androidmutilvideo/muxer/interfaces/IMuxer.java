package com.maniu.androidmutilvideo.muxer.interfaces;

import android.media.MediaCodec;

import com.maniu.androidmutilvideo.encoder.StreamPublisherParam;

public interface IMuxer
{

    int open(StreamPublisherParam params);

//  例如 直播写流数据 和 写文件数据相同
    void writeVideo(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo);

    void writeAudio(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo);

    int close();

}
