package com.maniu.mediavideomaniu.muxer.interfaces;

import android.media.MediaCodec;

import com.maniu.mediavideomaniu.encoder.StreamPublisher;
import com.maniu.mediavideomaniu.encoder.StreamPublisherParam;

public interface IMuxer
{

    int open(StreamPublisherParam params);

    void writeVideo(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo);

    void writeAudio(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo);

    int close();

}
