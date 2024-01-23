package com.maniu.androidmutilvideo.encoder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;

public class StreamPublisherParam {

    public int width = 640;
    public int height = 480;
    public int videoBitRate = 2949120;
    public int frameRate = 30;
    public int iframeInterval = 5;



    public int samplingRate = 44100;
    public int audioBitRate = 192000;
    public int audioSource;
    public int channelCfg = AudioFormat.CHANNEL_IN_STEREO;

    public String videoMIMEType = "video/avc";
    public String audioMIME = "audio/mp4a-latm";
    public int audioBufferSize;

    public String outputFilePath;
    public String outputUrl;
    private MediaFormat videoOutputMediaFormat;
    private MediaFormat audioOutputMediaFormat;

    private int initialTextureCount = 1;

    public StreamPublisherParam() {
        this(640, 480, 2949120, 30, 5, 44100, 192000, MediaRecorder.AudioSource.MIC, AudioFormat.CHANNEL_IN_STEREO);
    }

    private StreamPublisherParam(int width, int height, int videoBitRate, int frameRate,
                                 int iframeInterval, int samplingRate, int audioBitRate, int audioSource, int channelCfg) {
        this.width = width;
        this.height = height;
        this.videoBitRate = videoBitRate;
        this.frameRate = frameRate;
        this.iframeInterval = iframeInterval;
        this.samplingRate = samplingRate;
        this.audioBitRate = audioBitRate;
        this.audioBufferSize = AudioRecord.getMinBufferSize(samplingRate, channelCfg, AudioFormat.ENCODING_PCM_16BIT) * 2;
        this.audioSource = audioSource;
        this.channelCfg = channelCfg;
    }

    /**
     *
     * @param initialTextureCount Default is 1
     */
    public void setInitialTextureCount(int initialTextureCount) {
        if (initialTextureCount < 1) {
            throw new IllegalArgumentException("initialTextureCount must >= 1");
        }
        this.initialTextureCount = initialTextureCount;
    }

    public int getInitialTextureCount() {
        return initialTextureCount;
    }

    public MediaFormat createVideoMediaFormat() {
        MediaFormat format = MediaFormat.createVideoFormat(videoMIMEType, width, height);

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, videoBitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iframeInterval);
        return format;
    }

    public MediaFormat createAudioMediaFormat() {
        MediaFormat format = MediaFormat.createAudioFormat(audioMIME, samplingRate, 2);
        format.setInteger(MediaFormat.KEY_BIT_RATE, audioBitRate);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, audioBufferSize);

        return format;
    }

    public void setVideoOutputMediaFormat(MediaFormat videoOutputMediaFormat) {
        this.videoOutputMediaFormat = videoOutputMediaFormat;
    }

    public void setAudioOutputMediaFormat(MediaFormat audioOutputMediaFormat) {
        this.audioOutputMediaFormat = audioOutputMediaFormat;
    }

    public MediaFormat getVideoOutputMediaFormat() {
        return videoOutputMediaFormat;
    }

    public MediaFormat getAudioOutputMediaFormat() {
        return audioOutputMediaFormat;
    }

    public static class Builder {
        private int width = 640;
        private int height = 480;
        private int videoBitRate = 2949120;
        private int frameRate = 30;
        private int iframeInterval = 5;
        private int samplingRate = 44100;
        private int audioBitRate = 192000;
        private int audioSource = MediaRecorder.AudioSource.MIC;
        private int channelCfg = AudioFormat.CHANNEL_IN_STEREO;

        public Builder setWidth(int width) {
            this.width = width;
            return this;
        }

        public Builder setHeight(int height) {
            this.height = height;
            return this;
        }

        public Builder setVideoBitRate(int videoBitRate) {
            this.videoBitRate = videoBitRate;
            return this;
        }

        public Builder setFrameRate(int frameRate) {
            this.frameRate = frameRate;
            return this;
        }

        public Builder setIframeInterval(int iframeInterval) {
            this.iframeInterval = iframeInterval;
            return this;
        }

        public Builder setSamplingRate(int samplingRate) {
            this.samplingRate = samplingRate;
            return this;
        }

        public Builder setAudioBitRate(int audioBitRate) {
            this.audioBitRate = audioBitRate;
            return this;
        }

        public Builder setAudioSource(int audioSource) {
            this.audioSource = audioSource;
            return this;
        }

        public Builder setChannelCfg(int channelCfg) {
            this.channelCfg = channelCfg;
            return this;
        }

        public StreamPublisherParam createStreamPublisherParam() {
            return new StreamPublisherParam(width, height, videoBitRate, frameRate, iframeInterval, samplingRate, audioBitRate, audioSource, channelCfg);
        }
    }
}
