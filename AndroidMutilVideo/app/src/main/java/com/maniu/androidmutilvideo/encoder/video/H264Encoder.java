package com.maniu.androidmutilvideo.encoder.video;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.view.Surface;

import com.maniu.androidmutilvideo.encoder.MediaCodecInputStream;
import com.maniu.androidmutilvideo.encoder.StreamPublisherParam;

import java.io.IOException;

public class H264Encoder {
    //openg 一个屏幕        surface拿到数据
    private Surface mInputSurface;

    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 30;               // 30fps
    private static final int IFRAME_INTERVAL = 5;
    MediaCodec mEncoder;
    private boolean isStart;

//     读取变流   内部
    private MediaCodecInputStream mediaCodecInputStream;
    public H264Encoder(StreamPublisherParam params) {
//        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, params.width,
//                params.height);
        MediaFormat format = params.createVideoMediaFormat();
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

        format.setInteger(MediaFormat.KEY_BIT_RATE, params.videoBitRate);

        format.setInteger(MediaFormat.KEY_FRAME_RATE, params.frameRate);
//        I帧
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, params.iframeInterval);
        try {
            mEncoder = MediaCodec.createEncoderByType(params.videoMIMEType);
            mInputSurface = mEncoder.createInputSurface();
            mEncoder.start();


        } catch (IOException e) {

            e.printStackTrace();
        }
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        mediaCodecInputStream = new MediaCodecInputStream(mEncoder, new MediaCodecInputStream.MediaFormatCallback() {
            @Override
            public void onChangeMediaFormat(MediaFormat mediaFormat) {
                params.setVideoOutputMediaFormat(mediaFormat);
            }
        });

    }
    public MediaCodecInputStream getMediaCodecInputStream() {
        return mediaCodecInputStream;
    }

    public void start() {

    }

    public void close() {
        if (!isStart) return;

        mediaCodecInputStream.close();

        synchronized (mEncoder) {
            mEncoder.stop();
            mEncoder.release();
        }
        isStart = false;
    }
    public boolean isStart() {
        return isStart;
    }


}
